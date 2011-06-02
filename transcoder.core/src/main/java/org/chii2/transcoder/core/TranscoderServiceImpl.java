package org.chii2.transcoder.core;

import org.apache.commons.io.FileUtils;
import org.chii2.medialibrary.api.persistence.entity.Movie;
import org.chii2.medialibrary.api.persistence.entity.MovieFile;
import org.chii2.transcoder.api.core.TranscoderProcess;
import org.chii2.transcoder.api.core.TranscoderService;
import org.chii2.transcoder.core.avidemux.*;
import org.chii2.transcoder.core.dlna.catalog.*;
import org.chii2.transcoder.core.dlna.codec.AudioCodec;
import org.chii2.transcoder.core.dlna.codec.Container;
import org.chii2.transcoder.core.dlna.codec.MIME;
import org.chii2.transcoder.core.dlna.codec.VideoCodec;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teleal.cling.support.model.dlna.DLNAProfiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Transcoder Server act as a main entrance for all kinds of transcoding engines.
 */
public class TranscoderServiceImpl implements TranscoderService {
    // Injected ConfigAdmin Service
    private ConfigurationAdmin configAdmin;
    // Audio Catalogs
    List<AudioCatalog> audioCatalogs = new ArrayList<AudioCatalog>();
    // Video Catalogs
    List<VideoCatalog> videoCatalogs = new ArrayList<VideoCatalog>();
    // Image Catalogs
    List<ImageCatalog> imageCatalogs = new ArrayList<ImageCatalog>();
    // Temp Directory
    private File tempDirectory = new File(System.getProperty("java.io.tmpdir"), "chii2");
    // Logger
    private Logger logger = LoggerFactory.getLogger("org.chii2.transcoder.core");

    /**
     * Life Cycle Init
     */
    @SuppressWarnings("unused")
    public void init() {
        logger.debug("Chii2 Transcoder Core Service init.");
        // Init DLNA Profile Catalogs
        audioCatalogs.add(new AC3());
        audioCatalogs.add(new AMR());
        audioCatalogs.add(new ATRAC3());
        audioCatalogs.add(new LPCM());
        audioCatalogs.add(new MP3());
        audioCatalogs.add(new WMA());
        audioCatalogs.add(new AAC());

        videoCatalogs.add(new MPEG1());
        videoCatalogs.add(new MPEG2());
        videoCatalogs.add(new MPEG4P2());
        videoCatalogs.add(new MPEG4P10());
        videoCatalogs.add(new WMV());

        imageCatalogs.add(new JPEG());
        imageCatalogs.add(new PNG());

        // Create Temp Directory if not exist
        if (tempDirectory.exists()) {
            try {
                FileUtils.forceDelete(tempDirectory);
            } catch (IOException e) {
                logger.warn("Can not delete temp directory: {}.", tempDirectory.getAbsolutePath());
            }
        }
        try {
            FileUtils.forceMkdir(tempDirectory);
        } catch (IOException e) {
            logger.warn("Can not create temp directory: {}.", tempDirectory.getAbsolutePath());
        }
    }

    /**
     * Life Cycle Destroy
     */
    @SuppressWarnings("unused")
    public void destroy() {
        logger.debug("Chii2 Transcoder Core Service destroy.");
        if (tempDirectory.exists()) {
            try {
                FileUtils.forceDelete(tempDirectory);
            } catch (IOException e) {
                logger.warn("Can not delete temp directory: {}.", tempDirectory.getAbsolutePath());
            }
        }
    }

    @Override
    public String getClientProfile(String userAgent) {
        if (userAgent != null && userAgent.contains("Xbox")) {
            return PROFILE_XBOX;
        } else {
            return PROFILE_COMMON;
        }
    }

    @Override
    public String getClientProfile(List<String> userAgent) {
        for (String info : userAgent) {
            if (info != null && info.contains("Xbox")) {
                return PROFILE_XBOX;
            }
        }
        return PROFILE_COMMON;
    }

    @Override
    public boolean isValidMedia(String client, Movie movie) {
        // Fields
        String container = movie.getFormat();
        String videoFormat = movie.getVideoFormat();
        String videoFormatProfile = movie.getVideoFormatProfile();
        int videoFormatVersion = movie.getVideoFormatVersion();
        String videoCodec = movie.getVideoCodec();
        long videoBitRate = movie.getVideoBitRate();
        int videoWidth = movie.getVideoWidth();
        int videoHeight = movie.getVideoHeight();
        float fps = movie.getVideoFps();
        String audioFormat = movie.getAudioFormat();
        String audioFormatProfile = movie.getAudioFormatProfile();
        int audioFormatVersion = movie.getAudioFormatVersion();
        String audioCodec = movie.getAudioCodec();
        int audioChannels = movie.getAudioChannels();
        // Conditions
        if (client.equals(PROFILE_XBOX)) {
            if (Container.match(container, Container.AVI) &&
                    VideoCodec.match(videoFormat, videoFormatProfile, videoFormatVersion, videoCodec, VideoCodec.MPEG4_P2) &&
                    ((AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.AC3) && audioChannels <= 6)
                            || AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.MP3)
                            || AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.MP3X)) &&
                    videoBitRate <= 5000000 && videoWidth <= 1280 && videoHeight <= 720 && fps <= 30) {
                return true;
            }
            if ((Container.match(container, Container.MPEG4) || Container.match(container, Container.QUICK_TIME)) &&
                    VideoCodec.match(videoFormat, videoFormatProfile, videoFormatVersion, videoCodec, VideoCodec.MPEG4_P2) &&
                    ((AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.AAC_LC) && audioChannels <= 2)) &&
                    videoBitRate <= 5000000 && videoWidth <= 1280 && videoHeight <= 720 && fps <= 30) {
                return true;
            }
            if ((Container.match(container, Container.MPEG4) || Container.match(container, Container.QUICK_TIME)) &&
                    VideoCodec.match(videoFormat, videoFormatProfile, videoFormatVersion, videoCodec, VideoCodec.MPEG4_P10) &&
                    ((AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.AAC_LC) && audioChannels <= 2)) &&
                    videoBitRate <= 10000000 && videoWidth <= 1920 && videoHeight <= 1080 && fps <= 30) {
                return true;
            }
            if (Container.match(container, Container.WM) &&
                    VideoCodec.match(videoFormat, videoFormatProfile, videoFormatVersion, videoCodec, VideoCodec.WMV) &&
                    ((AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.WMA) && audioChannels <= 6)) &&
                    videoBitRate <= 15000000 && videoWidth <= 1920 && videoHeight <= 1080 && fps <= 30) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getVideoMIME(String container, String videoFormat, String videoFormatProfile, int videoFormatVersion, String videoCodec) {
        return MIME.getVideoMime(container, videoFormat, videoFormatProfile, videoFormatVersion, videoCodec);
    }

    @Override
    public DLNAProfiles getAudioProfile(String container, String audioFormat, String audioFormatProfile, int audioFormatVersion, String audioCodec, long audioBitRate, long audioSampleBitRate, int audioChannels) {
        DLNAProfiles profile = null;
        for (AudioCatalog audioCatalog : audioCatalogs) {
            profile = audioCatalog.resolve(container, audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, audioBitRate, audioSampleBitRate, audioChannels);
            if (profile != null) break;
        }
        return profile;
    }

    @Override
    public DLNAProfiles getVideoProfile(String container, String videoFormat, String videoFormatProfile, int videoFormatVersion, String videoCodec, long videoBitRate, int videoWidth, int videoHeight, float fps,
                                        String audioFormat, String audioFormatProfile, int audioFormatVersion, String audioCodec, long audioBitRate, long audioSampleBitRate, int audioChannels) {
        DLNAProfiles audioProfile = this.getAudioProfile(container, audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, audioBitRate, audioSampleBitRate, audioChannels);
        DLNAProfiles profile = null;
        for (VideoCatalog videoCatalog : videoCatalogs) {
            profile = videoCatalog.resolve(container, videoFormat, videoFormatProfile, videoFormatVersion, videoCodec, videoBitRate, videoWidth, videoHeight, fps,
                    audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, audioBitRate, audioSampleBitRate, audioChannels, audioProfile);
            if (profile != null) break;
        }
        return profile;
    }

    @Override
    public DLNAProfiles getImageProfile(String imageType, int imageWidth, int imageHeight) {
        DLNAProfiles profile = null;
        for (ImageCatalog imageCatalog : imageCatalogs) {
            profile = imageCatalog.resolve(imageType, imageWidth, imageHeight);
            if (profile != null) break;
        }
        return profile;
    }

    @Override
    public DLNAProfiles getVideoTranscodedProfile(String client, Movie movie) {
        String videoFormat = movie.getVideoFormat();
        String videoFormatProfile = movie.getVideoFormatProfile();
        int videoFormatVersion = movie.getVideoFormatVersion();
        String videoCodec = movie.getVideoCodec();
        String audioFormat = movie.getAudioFormat();
        String audioFormatProfile = movie.getAudioFormatProfile();
        int audioFormatVersion = movie.getAudioFormatVersion();
        String audioCodec = movie.getAudioCodec();
        if (client.equals(PROFILE_XBOX)) {
            if (VideoCodec.match(videoFormat, videoFormatProfile, videoFormatVersion, videoCodec, VideoCodec.MPEG4_P2)) {
                if (AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.AC3)) {
                    // TODO: This may not correct, but DLNA doesn't have a profile with MPEG4 Part2 & AVI & AC3
                    return DLNAProfiles.MPEG4_P2_TS_ASP_AC3_ISO;
                } else if (AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.MP3X)) {
                    // TODO: This may not correct, but DLNA doesn't have a profile with MPEG4 Part2 & AVI & MP3
                    return DLNAProfiles.MPEG4_P2_TS_ASP_MPEG1_L3_ISO;
                } else if (AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.AAC_LC)) {
                    return DLNAProfiles.MPEG4_P2_MP4_ASP_AAC;
                } else {
                    return DLNAProfiles.MPEG4_P2_MP4_ASP_AAC;
                }
            } else if (VideoCodec.match(videoFormat, videoFormatProfile, videoFormatVersion, videoCodec, VideoCodec.MPEG4_P10)) {
                return DLNAProfiles.AVC_MP4_HP_HD_AAC;
            } else {
                return DLNAProfiles.AVC_MP4_HP_HD_AAC;
            }
        }

        return DLNAProfiles.NONE;
    }

    @Override
    public String getTranscodedMIME(String client, Movie movie) {
        if (client.equals(PROFILE_XBOX)) {
            return "video/mp4";
        }

        return null;
    }

    @Override
    public String getVideoTranscodedCodec(String client, Movie movie) {
        String videoFormat = movie.getVideoFormat();
        String videoFormatProfile = movie.getVideoFormatProfile();
        int videoFormatVersion = movie.getVideoFormatVersion();
        String videoCodec = movie.getVideoCodec();
        if (client.equals(PROFILE_XBOX)) {
            if (VideoCodec.match(videoFormat, videoFormatProfile, videoFormatVersion, videoCodec, VideoCodec.MPEG4_P2) ||
                    VideoCodec.match(videoFormat, videoFormatProfile, videoFormatVersion, videoCodec, VideoCodec.MPEG4_P10)) {
                return videoCodec;
            } else {
                return "h264";
            }
        }

        return null;
    }

    @Override
    public List<TranscoderProcess> getTranscodedProcesses(String client, Movie movie) {
        return null;
//        // Fields
//        String container = movie.getFormat();
//        String videoFormat = movie.getVideoFormat();
//        String videoFormatProfile = movie.getVideoFormatProfile();
//        int videoFormatVersion = movie.getVideoFormatVersion();
//        String videoCodec = movie.getVideoCodec();
//        long videoBitRate = movie.getVideoBitRate();
//        int videoWidth = movie.getVideoWidth();
//        int videoHeight = movie.getVideoHeight();
//        float fps = movie.getVideoFps();
//        String audioFormat = movie.getAudioFormat();
//        String audioFormatProfile = movie.getAudioFormatProfile();
//        int audioFormatVersion = movie.getAudioFormatVersion();
//        String audioCodec = movie.getAudioCodec();
//        int audioChannels = movie.getAudioChannels();
//        // Script File
//        List<File> inputFiles = new ArrayList<File>();
//        for (MovieFile movieFile : movie.getFiles()) {
//            inputFiles.add(movieFile.getFile());
//        }
//        File scriptFile = new File(tempDirectory, UUID.randomUUID().toString() + ".script");
//        File outputFile = null;
//        ScriptTemplate scriptTemplate = null;
//        // Conditions
//        if (client.equals(PROFILE_XBOX)) {
//            if (VideoCodec.match(videoFormat, videoFormatProfile, videoFormatVersion, videoCodec, VideoCodec.MPEG4_P2) &&
//                    videoBitRate <= 5000000 && videoWidth <= 1280 && videoHeight <= 720 && fps <= 30) {
//                if (AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.AC3) && audioChannels <= 6) {
//                    outputFile = new File(tempDirectory, UUID.randomUUID().toString() + ".avi");
//                    scriptTemplate = new ScriptTemplate(inputFiles, outputFile, scriptFile, new VideoCopyConfig(), new AudioCopyConfig());
//                    scriptTemplate.setContainer("AVI");
//                } else if (AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.MP3)
//                        || AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.MP3X)) {
//                    outputFile = new File(tempDirectory, UUID.randomUUID().toString() + ".avi");
//                    scriptTemplate = new ScriptTemplate(inputFiles, outputFile, scriptFile, new VideoCopyConfig(), new AudioCopyConfig());
//                    scriptTemplate.setContainer("AVI");
//                } else if (AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.AAC_LC) && audioChannels <= 2) {
//                    outputFile = new File(tempDirectory, UUID.randomUUID().toString() + ".mp4");
//                    scriptTemplate = new ScriptTemplate(inputFiles, outputFile, scriptFile, new VideoCopyConfig(), new AudioCopyConfig());
//                    scriptTemplate.setContainer("MP4");
//                } else {
//                    outputFile = new File(tempDirectory, UUID.randomUUID().toString() + ".mp4");
//                    scriptTemplate = new ScriptTemplate(inputFiles, outputFile, scriptFile, new VideoCopyConfig(), new FaacConfig());
//                    scriptTemplate.setContainer("MP4");
//                }
//            } else if (VideoCodec.match(videoFormat, videoFormatProfile, videoFormatVersion, videoCodec, VideoCodec.MPEG4_P10) &&
//                    videoBitRate <= 10000000 && videoWidth <= 1920 && videoHeight <= 1080 && fps <= 30) {
//                if (AudioCodec.match(audioFormat, audioFormatProfile, audioFormatVersion, audioCodec, AudioCodec.AAC_LC) && audioChannels <= 2) {
//                    outputFile = new File(tempDirectory, UUID.randomUUID().toString() + ".mp4");
//                    scriptTemplate = new ScriptTemplate(inputFiles, outputFile, scriptFile, new VideoCopyConfig(), new AudioCopyConfig());
//                    scriptTemplate.setContainer("MP4");
//                } else {
//                    outputFile = new File(tempDirectory, UUID.randomUUID().toString() + ".mp4");
//                    scriptTemplate = new ScriptTemplate(inputFiles, outputFile, scriptFile, new VideoCopyConfig(), new FaacConfig());
//                    scriptTemplate.setContainer("MP4");
//                }
//            } else {
//                outputFile = new File(tempDirectory, UUID.randomUUID().toString() + ".mp4");
//                scriptTemplate = new ScriptTemplate(inputFiles, outputFile, scriptFile, getX264Fast(), new FaacConfig());
//                scriptTemplate.setContainer("MP4");
//            }
//        }
//
//        return new AViDemuxProcess(scriptTemplate);
    }

    @Override
    public String getImageTranscodedType(String client, String imageType) {
        if ("GIF".equalsIgnoreCase(imageType) || "GIFf".equalsIgnoreCase(imageType)) {
            return IMAGE_TYPE_GIF;
        } else if ("JPEG".equalsIgnoreCase(imageType) || "JPG".equalsIgnoreCase(imageType)) {
            return IMAGE_TYPE_JPEG;
        } else if ("PNG".equalsIgnoreCase(imageType) || "PNGf".equalsIgnoreCase(imageType)) {
            return IMAGE_TYPE_PNG;
        } else {
            return IMAGE_TYPE_JPEG;
        }
    }

    @Override
    public String getImageTranscodedMime(String client, String imageType) {
        if ("JPEG".equalsIgnoreCase(imageType) || "JPG".equalsIgnoreCase(imageType)) {
            return "image/jpeg";
        } else if ("PNG".equalsIgnoreCase(imageType) || "PNGf".equalsIgnoreCase(imageType)) {
            return "image/png";
        } else {
            return "image/jpeg";
        }
    }

    @Override
    public File getImageTranscodedFile(String client, String imageType, File imageFile) {
        return imageFile;
    }

    private X264Config getX264Fast() {
        X264Config config = new X264Config();
        config.setMbTree("false");
        config.setMotionEstimationMethod("diamond");
        config.setSubpixelRefinement("2");
        config.setMotionVectorSearchRange("16");
        config.setDirectPredictionMode("auto");
        config.setWeightedPrediction("false");
        config.setWeightedPredictionPframes("none");
        config.setDct8x8("false");
        config.setPartitionP8x8("false");
        config.setPartitionB8x8("false");
        config.setPartitionP4x4("false");
        config.setPartitionI8x8("false");
        config.setPartitionI4x4("false");
        config.setCabac("false");
        config.setbFrameReferences("none");
        config.setMixedReferences("false");
        config.setChromaMotionEstimation("false");
        config.setTrellis("disabled");
        return config;
    }

    private X264Config getX264Medium() {
        X264Config config = new X264Config();
        config.setMbTree("true");
        config.setFrametypeLookahead("40");
        config.setMotionEstimationMethod("hexagonal");
        config.setSubpixelRefinement("6");
        config.setMotionVectorSearchRange("16");
        config.setDirectPredictionMode("auto");
        config.setWeightedPrediction("true");
        config.setWeightedPredictionPframes("smart");
        config.setDct8x8("false");
        config.setPartitionP8x8("true");
        config.setPartitionB8x8("true");
        config.setPartitionP4x4("false");
        config.setPartitionI8x8("false");
        config.setPartitionI4x4("false");
        config.setCabac("false");
        config.setbFrameReferences("none");
        config.setMixedReferences("false");
        config.setChromaMotionEstimation("false");
        config.setTrellis("disabled");
        return config;
    }

    private X264Config getX264Slow() {
        X264Config config = new X264Config();
        config.setMbTree("true");
        config.setFrametypeLookahead("40");
        config.setMotionEstimationMethod("multi-hexagonal");
        config.setSubpixelRefinement("9");
        config.setMotionVectorSearchRange("24");
        config.setDirectPredictionMode("auto");
        config.setWeightedPrediction("true");
        config.setWeightedPredictionPframes("smart");
        config.setDct8x8("true");
        config.setPartitionP8x8("true");
        config.setPartitionB8x8("true");
        config.setPartitionP4x4("true");
        config.setPartitionI8x8("true");
        config.setPartitionI4x4("true");
        config.setCabac("true");
        config.setbFrameReferences("normal");
        config.setMixedReferences("true");
        config.setChromaMotionEstimation("true");
        config.setTrellis("finalMacroblock");
        return config;
    }

    /**
     * Inject Config Admin
     *
     * @param configAdmin Config Admin
     */
    @SuppressWarnings("unused")
    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }
}
