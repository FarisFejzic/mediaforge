package com.mediaforge.worker.ffmpeg;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class FfmpegRunner {

    private static final long TIMEOUT_SECONDS = 60;

    public CommandResult run(List<String> command) {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            Process process = new ProcessBuilder(command).start();

            try (InputStream out = process.getInputStream();
                 InputStream err = process.getErrorStream()) {

                var outReader = executor.submit(() -> readAll(out));
                var errReader = executor.submit(() -> readAll(err));

                boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new FfmpegException("Command timed out after " + TIMEOUT_SECONDS + "s: " + command);
                }

                String stdout = outReader.get();
                String stderr = errReader.get();

                return new CommandResult(process.exitValue(), stdout, stderr);
            }
        } catch (IOException e) {
            throw new FfmpegException("Failed to start command: " + command, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FfmpegException("Interrupted while running command: " + command, e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new FfmpegException("Failed reading command output: " + command, e.getCause());
        }
    }

    private String readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        stream.transferTo(buffer);
        return buffer.toString();
    }
}