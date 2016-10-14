package go;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by niwei on 16/7/20.
 */
public class CompareHttpLog {

    private static final String lineSeparator = System.getProperty("line.separator");

    /**
     * HTTP解析时用到的分割器
     */
    private static final Splitter httpParseSplitter = Splitter.on(" ");

    /**
     * properties文件解析时用到的分割器
     */
    private static final Splitter propertiesParseSplitter = Splitter.on(",");

    private String leftFileName;

    private String rightFileName;

    private File resultFile;

    private Map<String, String> urlJsonIgnorePath = new HashMap<>();

    /**
     * @param leftFileName             比较文件路径
     * @param rightFileName            被比较文件路径
     * @param resultFileName           输出的结果文件路径
     * @param urlConfigurePropertyFile 用于文件响应内容比较时的URL黑名单配置
     * @throws IOException
     */
    public CompareHttpLog(String leftFileName, String rightFileName, String resultFileName, String urlConfigurePropertyFile) throws IOException {
        this.leftFileName = leftFileName;
        this.rightFileName = rightFileName;

        /**
         * 比较结果文件处理
         */
        this.resultFile = new File(resultFileName);
        Files.write("Http compare result", resultFile, Charsets.UTF_8);
        Files.append(lineSeparator, resultFile, Charsets.UTF_8);

        Properties properties = new Properties();
        properties.load(Class.class.getClassLoader().getSystemResourceAsStream(urlConfigurePropertyFile));
        for (String url : properties.stringPropertyNames()) {
            urlJsonIgnorePath.put(url, properties.getProperty(url));
        }
    }

    /**
     * 将内容追加入文件
     *
     * @param contents
     * @throws IOException
     */
    private void append(final String contents) throws IOException {
        Files.append(contents, resultFile, Charsets.UTF_8);
        Files.append(lineSeparator, resultFile, Charsets.UTF_8);
    }

    /**
     * 将文件解析成对象列表
     *
     * @param fileName
     * @return
     * @throws Exception
     */
    private List<ReqRespEntity> parseFile(String fileName) throws Exception {
        List<ReqRespEntity> readLines = Files.readLines(new File(fileName), Charsets.UTF_8, new LineProcessor<List<ReqRespEntity>>() {
            private List<ReqRespEntity> result = Lists.newArrayList();

            /**
             * 每一个请求响应解析结果所保存的对象
             */
            private ReqRespEntity entity;

            /**
             * 标记解析请求响应的次数值,初始从1开始,每次碰到三个特殊的unicde字符开始的行自增1,
             * 这样可以根据奇数偶数来判断当前是在处理请求还是在处理响应的内容
             */
            private int processReqResp = 1;

            /**
             * 标记当前处理的是从实体开始的第几行(每个实体以三个特殊的unicde字符开始)
             */
            private int lineNumber = 0;

            /**
             * 标志是否在处理请求体,用于保存POST请求的参数信息
             * 根据HTTP协议描述:
             *  请求消息和响应消息都是由开始行，消息报头（可选），空行（只有CRLF的行），消息正文（可选）组成
             *
             */
            private boolean isRequestBody = false;

            /**
             * 标志是否在处理响应体,
             * 根据HTTP协议描述:
             *  请求消息和响应消息都是由开始行，消息报头（可选），空行（只有CRLF的行），消息正文（可选）组成
             *
             */
            private boolean isResponseBody = false;

            /**
             * 标记解析响应的Chunked编码的内容值,初始从0开始
             * 这样可以根据奇数偶数来判断当前是在处理Chunked头还是Chunked的内容
             */
            private int processChunkedContent = 0;

            public boolean processLine(String line) throws IOException {
                if (line.startsWith("\uD83D\uDC35\uD83D\uDE48\uD83D\uDE49")) {
                    processReqResp++;//自增1
                    lineNumber = 0;//lineNumber值复位为0
                } else {
                    lineNumber++;

                    if ((processReqResp & 1) != 0) {//在处理请求
                        if (lineNumber == 1) {//第一行,获取到标志ID
                            entity = new ReqRespEntity();
                            entity.setId(Iterables.get(httpParseSplitter.split(line), 1));//第二项表示标志ID
                            result.add(entity);
                            isRequestBody = false;
                            isResponseBody = false;
                            processChunkedContent = 0;
                        } else if (lineNumber == 2) {//第二行,请求URL
                            entity.getRequestUrl().append(Iterables.get(httpParseSplitter.split(line), 1));//第二项表示请求URL
                        } else if (line.equals("")) {
                            isRequestBody = true;
                        } else if (isRequestBody) {
                            entity.getRequestUrl().append("?").append(line);
                        }
                    } else {//在处理响应
                        if (line.equals("")) {
                            isResponseBody = true;
                        } else if (line.startsWith("Transfer-Encoding")) {
                            entity.setResponseTransferEncoding(Iterables.get(httpParseSplitter.split(line), 1));//第二项表示Transfer-Encoding的内容
                        } else if (line.startsWith("Content-Type")) {
                            entity.setResponseContentType(Iterables.get(httpParseSplitter.split(line), 1));//第二项表示Content-Type的内容
                            if (entity.getResponseContentType().startsWith("application/json")) {
                                entity.setResponseJson(true);//表示响应内容为json格式
                            }
                        } else if (line.startsWith("Content-Length")) {
                            entity.setResponseContentLength(Iterables.get(httpParseSplitter.split(line), 1));//第二项表示Content-Length的内容
                        } else if (isResponseBody) {
                            if (entity.getResponseTransferEncoding() != null) {//表示响应的内容是用Chunked编码
                                processChunkedContent++;
                                if ((processChunkedContent & 1) == 0) {//在处理Chunked内容
                                    entity.getResponseContent().append(line);
                                }
                            }
                            if (entity.getResponseContentLength() != null) {//表示响应的内容是用正常编码
                                entity.getResponseContent().append(line);
                            }
                        }
                    }
                }


                return true;
            }

            public List<ReqRespEntity> getResult() {
                return result;
            }
        });

        return readLines;
    }

    /**
     * 比较内容是否相同
     *
     * @param leftContent    左边的内容
     * @param rightContent   被比较的右边的内容
     * @param isJson         内容是否是json
     * @param jsonIgnorePath json内容中需要忽略比较的json路径
     * @return
     */
    private boolean isContentSame(String leftContent, String rightContent, boolean isJson, String jsonIgnorePath) {
        boolean result;

        if (isJson && (jsonIgnorePath != null)) {
            Set<String> ignorePathSet = new HashSet<>();

            propertiesParseSplitter.split(jsonIgnorePath).forEach(ignorePath -> {
                ignorePathSet.add(ignorePath);
            });

            JsonContentCompare jsonContentCompare = new JsonContentCompare(leftContent, rightContent, ignorePathSet);
            result = jsonContentCompare.compare();
        } else {
            result = leftContent.equals(rightContent);
        }

        return result;
    }

    /**
     * 比较HTTP日志文件
     *
     * @throws Exception
     */
    public void compare() throws Exception {
        List<ReqRespEntity> leftList = parseFile(leftFileName);
        append("left file 总行数:" + leftList.size());

        List<ReqRespEntity> rightList = parseFile(rightFileName);
        append("right file 总行数:" + rightList.size());

        Set<String> urlSet = urlJsonIgnorePath.keySet();

        for (int i = 0; i < leftList.size(); i++) {
            ReqRespEntity leftEntity = leftList.get(i);
            String leftUrl = leftEntity.getRequestUrl().toString();

            ReqRespEntity rightEntity = null;

            for (int j = 0; j < rightList.size(); j++) {
                if (leftUrl.equals(rightList.get(j).getRequestUrl().toString())) {
                    rightEntity = rightList.get(j);
                    break;
                }
            }
            if (rightEntity == null) {
                continue;
            }

            String rightUrl = rightEntity.getRequestUrl().toString();

            /**
             * 获取配置文件中与当前URL匹配的配置项
             */
            String jsonIgnorePath = null;
            for (String urlRegular : urlSet) {
                if (leftUrl.matches(urlRegular)) {
                    jsonIgnorePath = urlJsonIgnorePath.get(urlRegular);
                    break;
                }
            }

            String leftResponseContent = leftEntity.getResponseContent().toString();
            String rightResponseContent = rightEntity.getResponseContent().toString();

            /**
             * 将响应报文比较后不同的响应内容记录到结果文件
             */
            if (!isContentSame(leftResponseContent, rightResponseContent, leftEntity.isResponseJson(), jsonIgnorePath)) {
                append("===================================");
                append(leftUrl);
                append("");
                append(rightUrl);
                append("");
                append("left 响应内容 :");
                append(leftEntity.getResponseContent().toString());
                append("");
                append("right 响应内容 :");
                append(rightEntity.getResponseContent().toString());
                append("===================================");
                append("");
                append("");
                append("");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        CompareHttpLog compareHttpLog = new CompareHttpLog("/Users/niwei/Downloads/gor-test-2016-07-22-19.log",
                "/Users/niwei/Downloads/gor-online-2016-07-22-19.log",
                "/Users/niwei/Downloads/compare-2016-07-22.log",
                "go/urlJsonIgnorePath.properties");

        compareHttpLog.compare();

    }
}
