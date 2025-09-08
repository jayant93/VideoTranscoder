# 🎬 JavaCV Video Transcoder Prototype

This is a **prototype video transcoder** written in **Java** using [JavaCV](https://github.com/bytedeco/javacv) (FFmpeg + OpenCV bindings).  
It demonstrates how platforms like YouTube transcode uploaded videos into multiple **formats and resolutions** for adaptive playback.

---

## ✨ Features
- Decode any input video supported by FFmpeg (`.mp4`, `.mkv`, `.avi`, etc.).
- Re-encode into multiple renditions:
  - **MP4 1080p** (H.264 + AAC)
  - **MP4 720p** (H.264 + AAC)
  - **3GP 360p** (H.264 + AAC)
- Preserves **audio** and synchronizes it with each rendition.
- Demonstrates **frame resizing** (OpenCV) and **multi-output encoding**.
- Extensible: add more resolutions, codecs, or containers easily.

---

## 🛠️ Tech Stack
- **Java 11+**
- [JavaCV](https://github.com/bytedeco/javacv) (includes FFmpeg + OpenCV binaries)
- FFmpeg (shipped with JavaCV, no separate install required)

---

## 📂 Project Structure
.
├── src/
│ └── main/java/
│ └── TranscodeAndEncode.java # Main transcoder program
├── pom.xml # Maven dependencies
└── README.md

yaml
Copy code

---

## ⚡ Setup

1. **Clone the repo**
   ```bash
   git clone https://github.com/<your-username>/javacv-transcoder.git
   cd javacv-transcoder
Add Maven dependency (pom.xml)

xml
Copy code
<dependency>
  <groupId>org.bytedeco</groupId>
  <artifactId>javacv-platform</artifactId>
  <version>1.5.8</version>
</dependency>
Build the project

bash
Copy code
mvn clean install
▶️ Usage
Run the transcoder with:

bash
Copy code
java -cp target/classes:target/dependency/* TranscodeAndEncode <input-file> <output-dir>
Example:

bash
Copy code
java -cp target/classes:target/dependency/* TranscodeAndEncode ./samples/input.mp4 ./output
This will produce:

lua
Copy code
output/
 ├── output_1080p.mp4
 ├── output_720p.mp4
 └── output_360p.3gp
📊 Example Output
output_1080p.mp4 → 1920x1080, H.264, ~6 Mbps, AAC audio

output_720p.mp4 → 1280x720, H.264, ~3 Mbps, AAC audio

output_360p.3gp → 640x360, H.264, ~600 kbps, AAC audio

You can play them in VLC, ffplay, or any modern media player.

🔧 Extending the Prototype
Add more renditions by creating more FFmpegFrameRecorder instances.

Change codecs:

H.264 → avcodec.AV_CODEC_ID_H264

VP9 → avcodec.AV_CODEC_ID_VP9

AV1 → avcodec.AV_CODEC_ID_AV1

Change container formats (e.g., mp4, mkv, webm).

Integrate hardware acceleration (NVENC, VAAPI) for faster encoding.

⚠️ Notes
This is a prototype — not production-ready. For large-scale use, you’d:

Split jobs across workers (e.g., with a message queue + Kubernetes).

Add adaptive bitrate packaging (HLS/DASH) after encoding.

Handle errors, logging, and hardware acceleration.

The 3GP output uses H.264 + AAC for compatibility; older phones may expect H.263/AMR.

📜 License
MIT License — free to use and modify.

