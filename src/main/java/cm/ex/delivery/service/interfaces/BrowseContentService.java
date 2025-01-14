package cm.ex.delivery.service.interfaces;

import cm.ex.delivery.entity.BrowseContent;
import cm.ex.delivery.request.BrowseListDto;
import cm.ex.delivery.response.BasicResponse;

import java.util.List;

public interface BrowseContentService {

    public BasicResponse createBrowseContent(BrowseListDto browseListDto);

    public BasicResponse addItemToBrowseContent(String browseContentId, String itemId);

    public List<BrowseContent> listAllBrowseContentByOrder();

    public BrowseContent getBrowseContentById(String browseContentId);

    public BrowseContent getBrowseContentByTitle(String title);

    public BasicResponse updateOrder(int currentOrder, int newOrder);

    public BasicResponse removeBrowseContentItem(String browseContentId, String itemId);

    public BasicResponse removeBrowseContentById(String browseContentId);

}