client, err := speech.NewClient(ctx)
if err != nil {
        log.Fatal(err)
}
stream, err := client.StreamingRecognize(ctx)
if err != nil {
        log.Fatal(err)
}
// Send the initial configuration message.
if err := stream.Send(&speechpb.StreamingRecognizeRequest{
        StreamingRequest: &speechpb.StreamingRecognizeRequest_StreamingConfig{
                StreamingConfig: &speechpb.StreamingRecognitionConfig{
                        Config: &speechpb.RecognitionConfig{
                                Encoding:        speechpb.RecognitionConfig_LINEAR16,
                                SampleRateHertz: 16000,
                                LanguageCode:    "en-US",
                        },
                },
        },
}); err != nil {
        log.Fatal(err)
}

go func() {
        // Pipe stdin to the API.
        buf := make([]byte, 1024)
        for {
                n, err := os.Stdin.Read(buf)
                if err == io.EOF {
                        // Nothing else to pipe, close the stream.
                        if err := stream.CloseSend(); err != nil {
                                log.Fatalf("Could not close stream: %v", err)
                        }
                        return
                }
                if err != nil {
                        log.Printf("Could not read from stdin: %v", err)
                        continue
                }
                if err = stream.Send(&speechpb.StreamingRecognizeRequest{
                        StreamingRequest: &speechpb.StreamingRecognizeRequest_AudioContent{
                                AudioContent: buf[:n],
                        },
                }); err != nil {
                        log.Printf("Could not send audio: %v", err)
                }
        }
}()

for {
        resp, err := stream.Recv()
        if err == io.EOF {
                break
        }
        if err != nil {
                log.Fatalf("Cannot stream results: %v", err)
        }
        if err := resp.Error; err != nil {
                // Workaround while the API doesn't give a more informative error.
                if err.Code == 3 || err.Code == 11 {
                        log.Print("WARNING: Speech recognition request exceeded limit of 60 seconds.")
                }
                log.Fatalf("Could not recognize: %v", err)
        }
        for _, result := range resp.Results {
                fmt.Printf("Result: %+v\n", result)
        }
}
