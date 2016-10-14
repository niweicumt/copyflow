package go;

/**
 * Created by niwei on 16/7/20.
 */
public class ReqRespEntity {
    private String id;

    //请求URL,包含了GET请求URL和POST请求的URL及请求体
    private StringBuilder requestUrl = new StringBuilder();

    private String responseContentType;
    private String responseTransferEncoding;
    private boolean isResponseJson = false;
    private String responseContentLength;
    private StringBuilder responseContent = new StringBuilder();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public StringBuilder getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(StringBuilder requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }

    public String getResponseTransferEncoding() {
        return responseTransferEncoding;
    }

    public void setResponseTransferEncoding(String responseTransferEncoding) {
        this.responseTransferEncoding = responseTransferEncoding;
    }

    public boolean isResponseJson() {
        return isResponseJson;
    }

    public void setResponseJson(boolean responseJson) {
        isResponseJson = responseJson;
    }

    public String getResponseContentLength() {
        return responseContentLength;
    }

    public void setResponseContentLength(String responseContentLength) {
        this.responseContentLength = responseContentLength;
    }

    public StringBuilder getResponseContent() {
        return responseContent;
    }

    public void setResponseContent(StringBuilder responseContent) {
        this.responseContent = responseContent;
    }

    @Override
    public String toString() {
        return "ReqRespEntity{" +
                "id='" + id + '\'' +
                ", requestUrl=" + requestUrl +
                ", responseContentType='" + responseContentType + '\'' +
                ", responseTransferEncoding='" + responseTransferEncoding + '\'' +
                ", isResponseJson=" + isResponseJson +
                ", responseContentLength='" + responseContentLength + '\'' +
                ", responseContent=" + responseContent +
                '}';
    }
}
