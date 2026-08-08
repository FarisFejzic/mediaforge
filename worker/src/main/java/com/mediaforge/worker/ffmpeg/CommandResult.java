package com.mediaforge.worker.ffmpeg;

public record CommandResult(int exitCode, String stdout, String stderr) {
    public boolean isSuccess() {
        return exitCode == 0;
    }

    public String stderrTail() {
        if (stderr == null || stderr.isBlank()) return "";
        String[] lines = stderr.strip().split("\n");
        int take = Math.min(5, lines.length);
        StringBuilder sb = new StringBuilder();
        for (int i = lines.length - take; i < lines.length; i++) {
            sb.append(lines[i].strip()).append('\n');
        }
        return sb.toString().strip();
    }
}