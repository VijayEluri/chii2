package org.chii2.medialibrary.api.provider;

import java.io.File;
import java.util.List;

/**
 * Movie File Information Provider Service
 */
public interface MovieFileInfoProviderService {

    // Event Topic for movie file information provided
    public final static String MOVIE_FILE_INFO_PROVIDED_TOPIC = "org/chii2/medialibrary/provider/movie/FILE_INFO_PROVIDED";
    // Event Topic for movie file information failed (with parse or fetch)
    public final static String MOVIE_FILE_INFO_FAILED_TOPIC = "org/chii2/medialibrary/provider/movie/FILE_INFO_FAILED";
    // Movie file property key in the movie file info provided events, this should be the file list from the request
    public final static String MOVIE_FILE_PROPERTY = "movie_file";
    // Movie file information property key in the movie file info provided events, this should be the movie file information result from provider
    public final static String MOVIE_FILE_INFO_PROPERTY = "movie_file_info";
    // Exclude providers property key in the movie file info provided events, this is used in failed event, if this provider failed append itself to the exclude provider list
    public final static String EXCLUDE_PROVIDERS_PROPERTY = "exclude_providers";

    /**
     * Get the provider name
     *
     * @return Provider name
     */
    public String getProviderName();

    /**
     * Get Movie File Information.
     * This should be done in a asynchronous way, through Event Admin
     *
     * @param movieFile Movie File
     * @param excludeProviders Exclude Movie File Info Providers (Which may already failed)
     */
    public void getMovieFileInformation(File movieFile, List<String> excludeProviders);

    /**
     * Get Movie Files Information.
     * This should be done in a asynchronous way, through Event Admin
     *
     * @param movieFiles Image File List
     * @param excludeProviders Exclude Movie File Info Providers (Which may already failed)
     */
    public void getMovieFileInformation(List<File> movieFiles, List<String> excludeProviders);
}