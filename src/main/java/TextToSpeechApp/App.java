package TextToSpeechApp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        System.out.println("Text to Speech (English & Hindi Supported)");
        System.out.println("English Content:- How are you.");
        System.out.println("Hindi Content:- नमस्ते, आप कैसे है");
        System.out.println("---------------------------------------------");

        try (Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8.name())) {
            System.out.print("Enter text (English or Hindi): ");
            String text = sc.nextLine().trim();

            if (text.isEmpty()) {
                System.err.println("Error: Text cannot be empty.");
                return;
            }
            
            String culture = detectCulture(text);
            String os = System.getProperty("os.name").toLowerCase();
            String osVersion = System.getProperty("os.version").toLowerCase();

            System.out.printf("Detected OS: %s, Version: %s%n%n", os, osVersion);

            if (!culture.equals("en-US") && !culture.equals("hi-IN")) { 
                System.err.println("Sorry! Only English and Hindi are supported currently.");
                return;
            }

            if (os.contains("win")) {
                speakWindows(text, culture, osVersion);
            } else if (os.contains("mac")) {
                speakMac(text, culture, osVersion);
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                speakLinux(text, culture);
            } else {
                System.err.println("Unsupported OS for Text-to-Speech.");
            }

        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
    }

    // Windows PowerShell TTS with basic version check
    private static void speakWindows(String text, String culture, String version) {
	        try {
	            double ver = Double.parseDouble(version.split("\\.")[0]);
	            if (ver < 6.0) {
	                System.err.println("Windows version too old for TTS.");
	                return;
	            }
	        } catch (Exception ignored) {}

        String psScript = String.format(
            "$ErrorActionPreference='Stop';" +
            "Add-Type -AssemblyName System.Speech;" +
            "$s=New-Object System.Speech.Synthesis.SpeechSynthesizer;" +
            "$voices=$s.GetInstalledVoices()|ForEach-Object{$_.VoiceInfo};" +
            "$pick=($voices|Where-Object{$_.Culture.Name -like '%s'})|Select-Object -First 1;" +
            "if(-not $pick -and '%s' -eq 'hi-IN'){$pick=($voices|Where-Object{$_.Culture.Name -like 'hi-IN'})|Select-Object -First 1;}" +
            "if(-not $pick){$pick=($voices|Where-Object{$_.Culture.Name -like 'en-*'})|Select-Object -First 1;}" +
            "if(-not $pick){throw 'No suitable TTS voice installed.'};" +
            "$s.SelectVoice($pick.Name);" +
            "$s.Speak([System.Text.Encoding]::UTF8.GetString([System.Text.Encoding]::Default.GetBytes('%s')));",
            culture, culture, text
        );

        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-Command", psScript);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("PowerShell TTS failed. Check if voices are installed.");
            }
        } catch (IOException e) {
            System.err.println("PowerShell not found or inaccessible. " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("TTS operation was interrupted.");
        }
    }


    // macOS "say" command with voice fallback
    private static void speakMac(String text, String culture, String version) {
        String voice;
        if (culture.equals("hi-IN")) {
            // Older macOS versions may not have "Lekha" voice
            voice = getAvailableMacHindiVoice();
        } else {
            voice = "Alex"; // default English voice
        }

        String command = String.format("say -v %s \"%s\"", voice, escapeText(text));
        try {
            Process process = new ProcessBuilder("bash", "-c", command).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("macOS TTS failed. Check available voices using: say -v '?'");
            }
        } catch (IOException e) {
            System.err.println("Error running 'say' command: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operation interrupted.");
        }
    }

    // Linux using "espeak"
    private static void speakLinux(String text, String culture) {
        String lang = culture.equals("hi-IN") ? "hi" : "en";
        String command = String.format("espeak -v %s \"%s\"", lang, escapeText(text));

        try {
            Process process = new ProcessBuilder("bash", "-c", command).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Linux TTS failed. Make sure 'espeak' is installed.");
            }
        } catch (IOException e) {
            System.err.println("'espeak' not installed or cannot execute: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operation interrupted.");
        }
    }

    // Detect English vs Hindi
    private static String detectCulture(String s) {
        for (int i = 0; i < s.length(); i++) {
            int cp = s.codePointAt(i);
            if (cp >= 0x0900 && cp <= 0x097F) return "hi-IN";
        }
        return "en-US";
    }

    // Escape special chars for bash/say/espeak
    private static String escapeText(String text) {
        return text.replace("\"", "\\\"").replace("$", "\\$");
    }

    // Fallback for macOS Hindi voices
    private static String getAvailableMacHindiVoice() {
        // Lekha is available on macOS 10.15+; otherwise fallback to default voice
        try {
            Process process = new ProcessBuilder("bash", "-c", "say -v '?' | grep -i 'Lekha'").start();
            if (process.waitFor() == 0) {
                return "Lekha";
            }
        } catch (Exception ignored) {}
        return "Samantha"; // fallback voice for older macOS
    }
}
