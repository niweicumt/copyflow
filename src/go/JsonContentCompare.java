package go;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * json内容比较工具类
 * <p>
 * Created by niwei on 16/7/21.
 */
public class JsonContentCompare {
    private static final String PATH_HEAD = "JSONHEAD";//json路径头定义
    private static final String PATH_SPLITTER = "#";//json路径分隔符定义,选择正则表达式中不用的,避免解析正则表达式的时候处理麻烦

    private String json;//原始json字符串
    private String compareJson;//被比较的json字符串
    private List<String> jsonPathList;//保存原始json的多有解析过的value的路径
    private Set<String> ignorePahtSet;//比较路径黑名单,用于忽略某些路径值的比较

    public JsonContentCompare(String json, String compareJson, Set<String> ignorePahtSet) {
        this.json = json;
        this.compareJson = compareJson;
        this.jsonPathList = new ArrayList();
        if (ignorePahtSet == null) {
            ignorePahtSet = new HashSet<>();
        }
        this.ignorePahtSet = ignorePahtSet;
    }

    public boolean compare() {
        boolean result = false;

        if (json == null || json.trim().equals("") || compareJson == null || compareJson.trim().equals("")) {
            return result;
        }

        JSONObject jsonObj = new JSONObject(json);
        JSONObject compareJsonObj = new JSONObject(compareJson);
        String jsonPath = PATH_HEAD;
        if ((jsonObj != JSONObject.NULL) && (compareJsonObj != JSONObject.NULL)) {
            result = compareObject(jsonObj, compareJsonObj, jsonPath);
        }

        return result;
    }

    /**
     * json的路径是否包含在ignorePahtSet中
     *
     * @param jsonPath
     * @return
     */
    private boolean isInIgnorePathJudge(String jsonPath) {
        boolean result = false;
        for (String path : ignorePahtSet) {
            if (jsonPath.matches(path)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * json对象比较
     *
     * @param jsonObj        json对象
     * @param compareJsonObj 与json对象比较的json对象
     * @param jsonPath       json对象在原始json中所处的路径
     * @return 比较结果
     */
    private boolean compareObject(JSONObject jsonObj, JSONObject compareJsonObj, String jsonPath) {
        boolean result = true;

        Iterator<String> jsonKeys = jsonObj.keys();
        String parentPath = jsonPath;
        while (jsonKeys.hasNext()) {
            String jsonKey = jsonKeys.next();
            jsonPath = parentPath + PATH_SPLITTER + jsonKey;
            if (isInIgnorePathJudge(jsonPath)) {
                //如果包含在ignorePahtSet中则忽略本次比较
                continue;
            }

            if (!compareJsonObj.has(jsonKey)) {
                //如果被比较的json中没有该key直接返回false
                result = false;
                break;
            }
            Object compareJsonValue = compareJsonObj.get(jsonKey);
            Object jsonValue = jsonObj.get(jsonKey);

            //json的value通用比较方法
            result = compareJsonValue(jsonValue, compareJsonValue, jsonPath);

            if (!result) {
                //result等于false,直接跳出循环,不再继续比较
                break;
            }
        }

        return result;
    }

    /**
     * json数组比较
     *
     * @param jsonArr        json数组
     * @param compareJsonArr 与json数组比较的json数组
     * @param jsonPath       json数组在原始json中所处的路径
     * @return 比较结果
     */
    private boolean compareArray(JSONArray jsonArr, JSONArray compareJsonArr, String jsonPath) {
        boolean result = true;

        String parentPath = jsonPath;
        for (int i = 0; i < jsonArr.length(); i++) {
            jsonPath = parentPath + "[" + i + "]";

            Object jsonValue = jsonArr.get(i);
            Object compareJsonValue = compareJsonArr.get(i);
            result = compareJsonValue(jsonValue, compareJsonValue, jsonPath);

            if (!result) {
                //result等于false,直接跳出循环,不再继续比较
                break;
            }
        }
        return result;
    }

    /**
     * 普通json值比较
     *
     * @param jsonValue        json值
     * @param compareJsonValue 与json值比较的json值
     * @param jsonPath         json值在原始json中所处的路径
     * @return
     */
    private boolean comparePlain(Object jsonValue, Object compareJsonValue, String jsonPath) {
        boolean result = true;

        if (isInIgnorePathJudge(jsonPath)) {
            return result;
        }

        // 直接比较字符串内容
        if (jsonValue != null && !jsonValue.equals(compareJsonValue)) {
            result = false;
        }
        return result;
    }

    /**
     * 比较json的value,需要根据实际json类型执行不同的解析路径
     *
     * @param jsonValue        原始json解析后的value
     * @param compareJsonValue 原始被比较json解析后的value
     * @param jsonPath         jsonValue在原始json中所处的路径
     * @return
     */
    private boolean compareJsonValue(Object jsonValue, Object compareJsonValue, String jsonPath) {
        boolean result = true;

        if (isInIgnorePathJudge(jsonPath)) {
            return result;
        }

        if (jsonValue instanceof JSONArray) {
            if (!(compareJsonValue instanceof JSONArray)) {
                //如果两个json对象类型不一样,直接返回false
                result = false;
            } else {
                result = compareArray((JSONArray) jsonValue, (JSONArray) compareJsonValue, jsonPath);
            }
        } else if (jsonValue instanceof JSONObject) {
            if (!(compareJsonValue instanceof JSONObject)) {
                //如果两个json对象类型不一样,直接返回false
                result = false;
            } else {
                result = compareObject((JSONObject) jsonValue, (JSONObject) compareJsonValue, jsonPath);
            }
        } else {
            result = comparePlain(jsonValue, compareJsonValue, jsonPath);
        }
        this.jsonPathList.add(jsonPath);
        return result;
    }

    public List<String> getJsonPathList() {
        return jsonPathList;
    }

    public static void main(String[] args) throws Exception {
        String jsonStr1 = "{\"result\":\"ok\",\"msg\":\"操作成功\",\"code\":200,\"data\":{\"LOCATION_THIRD\":{\"promotionId\":0,\"linkUrl\":\"\",\"promotionType\":null,\"showImgUrl\":\"http://static2.8dol.com/homeAds/TOP/618zq.jpg\",\"homeShowType\":\"INVITE\",\"typeName\":\"邀请好友\",\"timeDiff\":0,\"secondKillType\":null},\"LOCATION_FIRST\":{\"promotionId\":102,\"linkUrl\":\"http://t.cn/RqFwUhW\",\"promotionType\":\"SECOND_KILL\",\"showImgUrl\":\"http://static2.8dol.com/homeAds/TOP/622ms.jpg\",\"homeShowType\":\"WEB_SITE\",\"typeName\":\"秒杀\",\"timeDiff\":26632127,\"secondKillType\":\"END\"},\"LOCATION_SECOND\":{\"promotionId\":6,\"linkUrl\":\"\",\"promotionType\":\"FULL_DISCOUNT\",\"showImgUrl\":\"http://static2.8dol.com/homeAds/TOP/622tg.jpg\",\"homeShowType\":\"PROMOTION\",\"typeName\":\"团购批发\",\"timeDiff\":0,\"secondKillType\":null}},\"rescode\":200}";

        String jsonStr2 = "{\"result\":\"ok\",\"msg\":\"操作成功\",\"code\":200,\"data\":{\"LOCATION_SECOND\":{\"promotionId\":102,\"linkUrl\":\"http://t.cn/RqFwUhW\",\"promotionType\":\"SECOND_KILL\",\"showImgUrl\":\"http://static2.8dol.com/homeAds/secondkill.jpg\",\"homeShowType\":\"PROMOTION\",\"typeName\":\"秒杀\",\"timeDiff\":26631850,\"secondKillType\":\"END\"},\"LOCATION_FIRST\":{\"promotionId\":0,\"linkUrl\":\"\",\"promotionType\":null,\"showImgUrl\":\"http://static2.8dol.com/homeAds/first.jpg\",\"homeShowType\":\"DAY\",\"typeName\":\"默认广告位1显示\",\"timeDiff\":0,\"secondKillType\":null},\"LOCATION_THIRD\":{\"promotionId\":0,\"linkUrl\":\"\",\"promotionType\":null,\"showImgUrl\":\"http://static2.8dol.com/homeAds/third.jpg\",\"homeShowType\":\"INVITE\",\"typeName\":\"默认广告位2显示\",\"timeDiff\":0,\"secondKillType\":null}},\"rescode\":200}";

        Set<String> blackPathSet = new HashSet<>();
        blackPathSet.add(PATH_HEAD + PATH_SPLITTER + ".*LOCATION_SECOND.*");
        blackPathSet.add(PATH_HEAD + PATH_SPLITTER + ".*LOCATION_THIRD.*");
        blackPathSet.add(PATH_HEAD + PATH_SPLITTER + ".*LOCATION_FIRST.*");

        JsonContentCompare jsonContentCompare = new JsonContentCompare(jsonStr1, jsonStr2, blackPathSet);

        System.out.println("对象比较结果:" + jsonContentCompare.compare());

        System.out.println("对象比较路径:");

        jsonContentCompare.getJsonPathList().forEach(path -> {
            System.out.println(path);
        });

        /*String jsonStr1 = "{\"result\":\"ok\",\"msg\":\"登录成功\",\"code\":200,\"data\":{\"username\":\"18625150155\",\"mobile\":\"18625150155\",\"email\":\"\",\"status\":1,\"head_ico\":\"\",\"lock_reason\":\"\",\"open_id_app\":\"\",\"union_id\":\"\",\"verify_code\":\"Toalzwd-gppx-CUEL2WFYDLFPKXIZ54URGAR6IM-xo\",\"bind_mobile\":1,\"isBind8Dol\":1,\"user_id\":\"a4b55123100c8fcb733e7ded03465967b58017b3\",\"is_Vip\":false,\"vip_expire_time\":\"\"},\"rescode\":200,\"timestamp\":1469009437228}";

        String jsonStr2 = "{\"result\":\"ok\",\"msg\":\"操作成功\",\"code\":200,\"data\":[],\"rescode\":200}";

        Set<String> blackPathSet = new HashSet<>();
        blackPathSet.add(".*#msg");
        blackPathSet.add(".*#data");
        blackPathSet.add(".*#timestamp");


        JsonContentCompare jsonContentCompare = new JsonContentCompare(jsonStr1, jsonStr2, blackPathSet);

        System.out.println("对象比较结果:" + jsonContentCompare.compare());

        System.out.println("对象比较路径:");

        jsonContentCompare.getJsonPathList().forEach(path -> {
            System.out.println(path);
        });*/
    }
}
