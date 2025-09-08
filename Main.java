package com.transcoder;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

import java.io.File;

/**
 * TranscodeAndEncode
 *
 * - Decodes input video with FFmpegFrameGrabber
 * - Encodes into multiple outputs (MP4 1080p, MP4 720p, 3GP 360p)
 *
 * Usage:
 *   java -cp <classpath-with-javacv> TranscodeAndEncode /path/to/input.mp4 /path/to/outdir
 *
 * Notes:
 * - This is a synchronous, single-process pipeline. For production you'd parallelize, add error handling and hardware accel.
 * - Ensure native FFmpeg (bundled with javacv-platform) supports chosen codecs/containers.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        String inputPath = "uploads/testVideo.mp4";
        String outDirPath = "transcode";
        File outDir = new File(outDirPath);
        if (!outDir.exists()) outDir.mkdirs();

        // Enable FFmpeg logs for better diagnostics
        FFmpegLogCallback.set();
        avutil.av_log_set_level(avutil.AV_LOG_INFO);

        // Open grabber
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputPath);
        grabber.start();

        System.out.println("Input format: " + grabber.getFormat());
        System.out.println("Input size: " + grabber.getImageWidth() + "x" + grabber.getImageHeight());
        System.out.println("Input fps: " + grabber.getFrameRate());
        System.out.println("Audio channels: " + grabber.getAudioChannels() + " sampleRate: " + grabber.getSampleRate());

        // Get source FPS, sample rate & channels
        double srcFps = (grabber.getFrameRate() > 0 ? grabber.getFrameRate() : 25.0);
        int audioChannels = grabber.getAudioChannels();
        int audioSampleRate = grabber.getSampleRate();

        // Prepare converters for resizing frames
        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();

        // Create recorders for each rendition
        // 1) MP4 1080p
        String out1080 = new File(outDir, "output_1080p.mp4").getAbsolutePath();
        FFmpegFrameRecorder r1080 = createRecorder(out1080, 1920, 1080, srcFps,
                avcodec.AV_CODEC_ID_H264, 6_000_000, audioSampleRate, audioChannels, avcodec.AV_CODEC_ID_AAC, "mp4");

        // 2) MP4 720p
        String out720 = new File(outDir, "output_720p.mp4").getAbsolutePath();
        FFmpegFrameRecorder r720 = createRecorder(out720, 1280, 720, srcFps,
                avcodec.AV_CODEC_ID_H264, 3_000_000, audioSampleRate, audioChannels, avcodec.AV_CODEC_ID_AAC, "mp4");

        // 3) 3GP 360p (lower bitrate)
        String out3gp = new File(outDir, "output_360p.3gp").getAbsolutePath();
        FFmpegFrameRecorder r3gp = createRecorder(out3gp, 640, 360, srcFps,
                avcodec.AV_CODEC_ID_H264, 600_000, audioSampleRate, audioChannels, avcodec.AV_CODEC_ID_AAC, "3gp");

        // Start recorders
        r1080.start();
        r720.start();
        r3gp.start();

        System.out.println("Recorders started.");

        // Main loop: grab frames and dispatch to recorders (video & audio)
        Frame frame;
        long frameCount = 0;
        try {
            while ((frame = grabber.grab()) != null) {
                frameCount++;

                // Video frame
                if (frame.image != null) {
                    // Convert Frame -> Mat for resizing
                    Mat srcMat = converterToMat.convert(frame);

                    // 1080: resize if necessary (or reuse if source is same size)
                    Mat mat1080 = new Mat();
                    if (srcMat.cols() != 1920 || srcMat.rows() != 1080) {
                        opencv_imgproc.resize(srcMat, mat1080, new Size(1920, 1080));
                    } else {
                        mat1080 = srcMat.clone();
                    }
                    Frame f1080 = converterToMat.convert(mat1080);
                    r1080.record(f1080);

                    // 720
                    Mat mat720 = new Mat();
                    opencv_imgproc.resize(srcMat, mat720, new Size(1280, 720));
                    Frame f720 = converterToMat.convert(mat720);
                    r720.record(f720);

                    // 360 / 3gp
                    Mat mat360 = new Mat();
                    opencv_imgproc.resize(srcMat, mat360, new Size(640, 360));
                    Frame f360 = converterToMat.convert(mat360);
                    r3gp.record(f360);

                    // release mats (native memory)
                    mat1080.release();
                    mat720.release();
                    mat360.release();
                    srcMat.release();

                    if (frameCount % 100 == 0) {
                        System.out.println("Processed video frames: " + frameCount);
                    }
                }

                // Audio frame / samples
                if (frame.samples != null) {
                    // Pass the same audio buffers to all recorders
                    // Using sample rate & channels from grabber ensures timestamps align
                    r1080.recordSamples(audioSampleRate, audioChannels, frame.samples);
                    r720.recordSamples(audioSampleRate, audioChannels, frame.samples);
                    r3gp.recordSamples(audioSampleRate, audioChannels, frame.samples);
                }
            }
        } finally {
            // Stop and release everything
            System.out.println("Finishing... total frames: " + frameCount);
            try {
                r1080.stop();
                r1080.release();
            } catch (Exception e) {
                System.err.println("Error stopping r1080: " + e.getMessage());
            }
            try {
                r720.stop();
                r720.release();
            } catch (Exception e) {
                System.err.println("Error stopping r720: " + e.getMessage());
            }
            try {
                r3gp.stop();
                r3gp.release();
            } catch (Exception e) {
                System.err.println("Error stopping r3gp: " + e.getMessage());
            }
            grabber.stop();
            grabber.release();
        }

        System.out.println("All done. Outputs:");
        System.out.println(" - " + out1080);
        System.out.println(" - " + out720);
        System.out.println(" - " + out3gp);
    }

    /**
     * Helper to create and configure FFmpegFrameRecorder
     */
    private static FFmpegFrameRecorder createRecorder(String filename,
                                                      int width, int height,
                                                      double fps,
                                                      int videoCodecId, int videoBitrate,
                                                      int audioSampleRate, int audioChannels,
                                                      int audioCodecId,
                                                      String format) {
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(filename, width, height, audioChannels);
        recorder.setFormat(format);
        // Video settings
        recorder.setVideoCodec(videoCodecId); // AV_CODEC_ID_H264
        recorder.setVideoBitrate(videoBitrate); // bits per second
        recorder.setFrameRate(fps);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); // compatibility

        // Encoder tune/preset can be set as options if needed (platform dependent)
        // e.g., recorder.setVideoOption("preset", "fast");

        // Audio settings
        recorder.setAudioCodec(audioCodecId); // AV_CODEC_ID_AAC
        recorder.setSampleRate(audioSampleRate);
        recorder.setAudioChannels(audioChannels);
        recorder.setAudioBitrate(128_000);

        // Container-level options for rate control
        // (maxrate/bufsize can be set via setVideoOption)
        // recorder.setVideoOption("maxrate", String.valueOf(videoBitrate * 1.5));
        // recorder.setVideoOption("bufsize", String.valueOf(videoBitrate * 2));

        return recorder;
    }
}
