package org.chii2.mediaserver.content.common.container;

import org.chii2.mediaserver.api.content.ContentManager;
import org.chii2.mediaserver.api.content.container.VisualContainer;
import org.chii2.mediaserver.content.common.CommonContentManager;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.SortCriterion;
import org.teleal.cling.support.model.WriteStatus;
import org.teleal.cling.support.model.container.Container;

import java.util.List;

/**
 * Image Container
 * Contains all containers and pictures represent folders in storage
 */
public class PicturesFoldersContainer extends Container implements VisualContainer {

    // Filter
    private String filter;
    // Total Child Count
    private long totalChildCount;

    /**
     * Constructor
     *
     * @param filter Content Filter
     */
    public PicturesFoldersContainer(String filter) {
        this(filter, CommonContentManager.PICTURES_FOLDERS_ID, CommonContentManager.PICTURES_ID);
    }

    /**
     * Constructor
     *
     * @param filter   Content Filter
     * @param id       Container ID
     * @param parentId Parent ID
     */
    public PicturesFoldersContainer(String filter, String id, String parentId) {
        super();

        this.filter = filter;

        // Pictures Folders Container ID: 16
        setId(id);
        // Parent container is Pictures Container
        setParentID(parentId);
        // Title TODO: This should be I18N
        setTitle("Pictures/Folders");
        // May used in Container Property Creator (part of UPnP protocol standard)
        setCreator("System");
        // May used in Container Property Clazz (part of UPnP protocol standard)
        setClazz(new DIDLObject.Class("object.container"));
        // Restricted
        setRestricted(true);
        // Searchable
        setSearchable(false);
        // Writable
        setWriteStatus(WriteStatus.NOT_WRITABLE);
    }

    @Override
    public long getTotalChildCount() {
        return this.totalChildCount;
    }

    @Override
    public void setTotalChildCount(long totalChildCount) {
        this.totalChildCount = totalChildCount;
    }

    @Override
    public void loadContents(long startIndex, long maxCount, SortCriterion[] orderBy, ContentManager contentManager) {
        // Load from library
        List<? extends VisualContainer> containers = contentManager.getPicturesStorageFolders(filter, startIndex, maxCount, orderBy);
        long count = contentManager.getPicturesStorageFoldersCount();
        // Add children
        if (containers != null && count > 0) {
            for (VisualContainer container : containers) {
                addContainer((Container) container);
            }
            setChildCount(containers.size());
            setTotalChildCount(count);
        } else {
            setChildCount(0);
            setTotalChildCount(0);
        }
    }
}
