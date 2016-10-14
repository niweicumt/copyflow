package go.middleware;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Gor中间件Java版本,增强的功能有:
 *
 * 1.在请求体中注入参数GorRequestId,用于请求回放时的原始请求比对
 * 2.支持根据url配置过滤请求和响应的输出
 * <p>
 * Created by niwei on 16/7/22.
 */
public class Stdout {
    private static final String SPLITTER_HEADER_BODY_SPLITTER = "\r\n\r\n";
    private static final String SPLITTER_HEAD_FIRST_LINE = "\n";
    private static final String SPLITTER_HEADER_ITEM = " ";
    /**
     * payload type, possible values: 1 - request, 2 - original response, 3 - replayed response
     */
    private static final String PAYLOAD_TYPE_REQUEST = "1";
    private static final String PAYLOAD_TYPE_ORIGINAL_RESPONSE = "2";

    /**
     * 定义新增加的requestId参数名称
     */
    private static String INJECT_TO_REQUEST_ENTITY_REQUEST_ID = "GorRequestId";

    /**
     * 定义需要输出的请求和响应的requestId
     */
    private static Set<String> recordRequestIds = new HashSet<>();

    /**
     * convert hex to string
     *
     * @param hexStr
     * @return
     * @throws Exception
     */
    public static String hexDecode(String hexStr) throws Exception {
        byte[] decodedHex = DatatypeConverter.parseHexBinary(hexStr);
        String decodedString = new String(decodedHex, "UTF-8");

        return decodedString;
    }

    /**
     * convert string to hex
     *
     * @param str
     * @return
     * @throws Exception
     */
    public static String encodeHex(String str) throws Exception {
        if (str == null) {
            return null;
        }
        byte[] strBytes = str.getBytes();
        String encodeString = DatatypeConverter.printHexBinary(strBytes);

        return encodeString;
    }

    private static String getRequestHeader(String key, String value) {
        StringBuilder result = new StringBuilder(SPLITTER_HEAD_FIRST_LINE);

        result.append(key).append(":").append(SPLITTER_HEADER_ITEM).append(value);

        return result.toString();
    }

    /**
     * gor原始内容增强
     *
     * @param content 原始的gor工具输出的内容
     * @param allowUrlRegular 允许记录文件的url正则表达式
     * @return 增强后输出的内容
     */
    public static String enhanceContent(String content, String allowUrlRegular) {
        if ((allowUrlRegular == null) || (allowUrlRegular.trim().equals(""))){
            allowUrlRegular = "*";
        }

        String result = content;

        /**
         * get first line content
         */
        String[] lines = content.split(SPLITTER_HEAD_FIRST_LINE);
        if (lines == null || lines.length <= 1) {
            return result;
        }
        String firstLine = lines[0];
        String secondLine = lines[1];

        String[] firstLineItems = firstLine.split(SPLITTER_HEADER_ITEM);
        if (firstLineItems.length != 3) {
            return result;
        } else {
            String payloadType = firstLineItems[0];
            String requestId = firstLineItems[1];

            if (PAYLOAD_TYPE_REQUEST.equals(payloadType)) {
                String[] secondLineItems = secondLine.split(SPLITTER_HEADER_ITEM);
                String url = secondLineItems[1];
                String uri = url;
                int urlIndex = url.indexOf("?");
                if (urlIndex > 0) {
                    uri = url.substring(0, urlIndex);
                }

                String requestIdPair = INJECT_TO_REQUEST_ENTITY_REQUEST_ID + "=" + requestId + "&";
                result = content.replaceFirst(SPLITTER_HEADER_BODY_SPLITTER, SPLITTER_HEADER_BODY_SPLITTER + requestIdPair);

                boolean isMatch = false;
                String[] allowUrls = allowUrlRegular.split(",");
                for (String allowUrl : allowUrls) {
                    if (uri.matches(allowUrl)){
                        recordRequestIds.add(requestId);
                        isMatch = true;
                        break;
                    }
                }
                if(!isMatch){
                    //URL不能匹配上的则不输出到文件
                    result = "";
                }

            } else if (PAYLOAD_TYPE_ORIGINAL_RESPONSE.equals(payloadType)) {
                if (recordRequestIds.contains(requestId)) {
                    recordRequestIds.remove(requestId);
                } else {//不再recordRequestIds记录中则不输出到文件
                    result = "";
                }
            }
        }

        return result;
    }

    /**
     * java go.GorEnhance
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        String line;
        StringBuilder allowUrlRegular = new StringBuilder();
        int bytesRead = 0;
        byte[] buffer = new byte[1024];

        try (BufferedInputStream bufferedInput = new BufferedInputStream(Class.class.getClassLoader().getSystemResourceAsStream("go/middleware/allow-url.txt"))) {
            while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                allowUrlRegular.append(new String(buffer, 0, bytesRead));
            }
        }

        BufferedReader stdin = new BufferedReader(new InputStreamReader(
                System.in));
        while ((line = stdin.readLine()) != null) {
            System.out.println(encodeHex(enhanceContent(hexDecode(line), allowUrlRegular.toString())));
        }

    }
}
