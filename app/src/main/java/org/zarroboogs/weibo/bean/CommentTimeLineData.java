
package org.zarroboogs.weibo.bean;

/**
 * User: qii Date: 13-4-8
 */
public class CommentTimeLineData {
    public CommentListBean cmtList;
    public TimeLinePosition position;

    public CommentTimeLineData(CommentListBean cmtList, TimeLinePosition position) {
        this.cmtList = cmtList;
        this.position = position;
    }
}
