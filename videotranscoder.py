import os
import ffmpeg
import sys

def transcode(input_path, output_dir):
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    outputs = [
        # (filename, width, height, video bitrate, container)
        ("output_1080p.mp4", 1920, 1080, "6000k", "mp4"),
        ("output_720p.mp4", 1280, 720, "3000k", "mp4"),
        ("output_360p.3gp", 640, 360, "600k", "3gp"),
    ]

    for filename, width, height, v_bitrate, container in outputs:
        output_path = os.path.join(output_dir, filename)
        (
            ffmpeg
            .input(input_path)
            .output(
                output_path,
                vcodec='libx264',
                acodec='aac',
                video_bitrate=v_bitrate,
                audio_bitrate='128k',
                format=container,
                vf=f'scale={width}:{height}',
                r=25
            )
            .run(overwrite_output=True)
        )
        print(f"Created: {output_path}")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python videotranscoder.py <input_file> <output_dir>")
        sys.exit(1)
    input_file = sys.argv[1]
    output_dir = sys.argv[2]
    transcode(input_file, output_dir)