package com.unfbx.chatgpt;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.unfbx.chatgpt.entity.chat.*;
import com.unfbx.chatgpt.function.KeyRandomStrategy;
import com.unfbx.chatgpt.interceptor.DynamicKeyOpenAiAuthInterceptor;
import com.unfbx.chatgpt.interceptor.OpenAILogger;
import com.unfbx.chatgpt.interceptor.OpenAiResponseInterceptor;
import com.unfbx.chatgpt.sse.ConsoleEventSourceListener;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 描述： 测试类
 *
 * @author https:www.unfbx.com
 * 2023-06-14
 */
@Slf4j
public class OpenAiClientFunctionTest {

    private OpenAiClient openAiClient;
    private OpenAiStreamClient openAiStreamClient;

    @Before
    public void before() {
        //可以为null
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890));
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(new OpenAILogger());
        //！！！！千万别再生产或者测试环境打开BODY级别日志！！！！
        //！！！生产或者测试环境建议设置为这三种级别：NONE,BASIC,HEADERS,！！！
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
//                .proxy(proxy)
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(new OpenAiResponseInterceptor())
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        openAiClient = OpenAiClient.builder()
                //支持多key传入，请求时候随机选择
                .apiKey(Arrays.asList("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"))
                //自定义key的获取策略：默认KeyRandomStrategy
                .keyStrategy(new KeyRandomStrategy())
                .authInterceptor(new DynamicKeyOpenAiAuthInterceptor())
                .okHttpClient(okHttpClient)
                //自己做了代理就传代理地址，没有可不不传,(关注公众号回复：openai ，获取免费的测试代理地址)
                .apiHost("https://dgr.life/")
                .build();

        openAiStreamClient = OpenAiStreamClient.builder()
                //支持多key传入，请求时候随机选择
                .apiKey(Arrays.asList("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"))
                //自定义key的获取策略：默认KeyRandomStrategy
                .keyStrategy(new KeyRandomStrategy())
                .authInterceptor(new DynamicKeyOpenAiAuthInterceptor())
                .okHttpClient(okHttpClient)
                //自己做了代理就传代理地址，没有可不不传,(关注公众号回复：openai ，获取免费的测试代理地址)
                .apiHost("https://dgr.life/")
                .build();
    }

    /**
     * 阻塞输出日志如下：
     *
     * [main] INFO com.unfbx.chatgpt.OpenAiClientFunctionTest - 自定义的方法返回值：词语：苹果
     *
     * 用途：苹果是一种水果，具有多种用途。以下是苹果的几种常见用途：
     *
     * 1. 直接吃：苹果可以直接食用，具有清爽的口感和丰富的营养成分，是一种健康的零食选择。
     *
     * 2. 做沙拉：苹果可以切成块状或丝状，加入其他蔬菜和调味料，制作成沙拉。苹果的甜脆口感可以为沙拉增添口感和风味。
     *
     * 3. 售卖：苹果是一种常见的水果，可以被商家售卖。人们可以购买苹果作为食物或礼物，满足自己或他人的需求。
     *
     * 总之，苹果是一种多功能的水果，可以直接食用，也可以用于制作沙拉，同时也是一种常见的商业商品。
     */
    @Test
    public void chatFunction() {

        //模型：GPT_3_5_TURBO_16K_0613
        Message message = Message.builder().role(Message.Role.USER).content("给我输出一个长度为2的中文词语，并解释下词语对应物品的用途").build();
        //属性一
        JSONObject wordLength = new JSONObject();
        wordLength.putOpt("type", "number");
        wordLength.putOpt("description", "词语的长度");
        //属性二
        JSONObject language = new JSONObject();
        language.putOpt("type", "string");
        language.putOpt("enum", Arrays.asList("zh", "en"));
        language.putOpt("description", "语言类型，例如：zh代表中文、en代表英语");
        //参数
        JSONObject properties = new JSONObject();
        properties.putOpt("wordLength", wordLength);
        properties.putOpt("language", language);
        Parameters parameters = Parameters.builder()
                .type("object")
                .properties(properties)
                .required(Arrays.asList("wordLength")).build();
        Functions functions = Functions.builder()
                .name("getOneWord")
                .description("获取一个指定长度和语言类型的词语")
                .parameters(parameters)
                .build();

        ChatCompletion chatCompletion = ChatCompletion
                .builder()
                .messages(Arrays.asList(message))
                .functions(Arrays.asList(functions))
                .functionCall("auto")
                .model(ChatCompletion.Model.GPT_3_5_TURBO_16K_0613.getName())
                .build();
        ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);

        ChatChoice chatChoice = chatCompletionResponse.getChoices().get(0);
        log.info("构造的方法值：{}", chatChoice.getMessage().getFunctionCall());
        log.info("构造的方法名称：{}", chatChoice.getMessage().getFunctionCall().getName());
        log.info("构造的方法参数：{}", chatChoice.getMessage().getFunctionCall().getArguments());
        WordParam wordParam = JSONUtil.toBean(chatChoice.getMessage().getFunctionCall().getArguments(), WordParam.class);
        String oneWord = getOneWord(wordParam);

        FunctionCall functionCall = FunctionCall.builder()
                .arguments(chatChoice.getMessage().getFunctionCall().getArguments())
                .name("getOneWord")
                .build();
        Message message2 = Message.builder().role(Message.Role.ASSISTANT).content("方法参数").functionCall(functionCall).build();
        String content
                = "{ " +
                "\"wordLength\": \"3\", " +
                "\"language\": \"zh\", " +
                "\"word\": \"" + oneWord + "\"," +
                "\"用途\": [\"直接吃\", \"做沙拉\", \"售卖\"]" +
                "}";
        Message message3 = Message.builder().role(Message.Role.FUNCTION).name("getOneWord").content(content).build();
        List<Message> messageList = Arrays.asList(message, message2, message3);
        ChatCompletion chatCompletionV2 = ChatCompletion
                .builder()
                .messages(messageList)
                .model(ChatCompletion.Model.GPT_3_5_TURBO_16K_0613.getName())
                .build();
        ChatCompletionResponse chatCompletionResponseV2 = openAiClient.chatCompletion(chatCompletionV2);
        log.info("自定义的方法返回值：{}",chatCompletionResponseV2.getChoices().get(0).getMessage().getContent());
    }


    /**
     * 流式输出最后输出日志如下
     * .........省略省略省略省略省略............
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据：{"id":"chatcmpl-7RXBSbHZPGCbiV9uO9iPlgoZ56t9y","object":"chat.completion.chunk","created":1686796770,"model":"gpt-3.5-turbo-16k-0613","choices":[{"index":0,"delta":{"content":"、"},"finish_reason":null}]}
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据：{"id":"chatcmpl-7RXBSbHZPGCbiV9uO9iPlgoZ56t9y","object":"chat.completion.chunk","created":1686796770,"model":"gpt-3.5-turbo-16k-0613","choices":[{"index":0,"delta":{"content":"水"},"finish_reason":null}]}
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据：{"id":"chatcmpl-7RXBSbHZPGCbiV9uO9iPlgoZ56t9y","object":"chat.completion.chunk","created":1686796770,"model":"gpt-3.5-turbo-16k-0613","choices":[{"index":0,"delta":{"content":"果"},"finish_reason":null}]}
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据：{"id":"chatcmpl-7RXBSbHZPGCbiV9uO9iPlgoZ56t9y","object":"chat.completion.chunk","created":1686796770,"model":"gpt-3.5-turbo-16k-0613","choices":[{"index":0,"delta":{"content":"摊"},"finish_reason":null}]}
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据：{"id":"chatcmpl-7RXBSbHZPGCbiV9uO9iPlgoZ56t9y","object":"chat.completion.chunk","created":1686796770,"model":"gpt-3.5-turbo-16k-0613","choices":[{"index":0,"delta":{"content":"等"},"finish_reason":null}]}
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据：{"id":"chatcmpl-7RXBSbHZPGCbiV9uO9iPlgoZ56t9y","object":"chat.completion.chunk","created":1686796770,"model":"gpt-3.5-turbo-16k-0613","choices":[{"index":0,"delta":{"content":"渠"},"finish_reason":null}]}
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据：{"id":"chatcmpl-7RXBSbHZPGCbiV9uO9iPlgoZ56t9y","object":"chat.completion.chunk","created":1686796770,"model":"gpt-3.5-turbo-16k-0613","choices":[{"index":0,"delta":{"content":"道"},"finish_reason":null}]}
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据：{"id":"chatcmpl-7RXBSbHZPGCbiV9uO9iPlgoZ56t9y","object":"chat.completion.chunk","created":1686796770,"model":"gpt-3.5-turbo-16k-0613","choices":[{"index":0,"delta":{"content":"进行"},"finish_reason":null}]}
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据：{"id":"chatcmpl-7RXBSbHZPGCbiV9uO9iPlgoZ56t9y","object":"chat.completion.chunk","created":1686796770,"model":"gpt-3.5-turbo-16k-0613","choices":[{"index":0,"delta":{"content":"销"},"finish_reason":null}]}
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据：{"id":"chatcmpl-7RXBSbHZPGCbiV9uO9iPlgoZ56t9y","object":"chat.completion.chunk","created":1686796770,"model":"gpt-3.5-turbo-16k-0613","choices":[{"index":0,"delta":{"content":"售"},"finish_reason":null}]}
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据：{"id":"chatcmpl-7RXBSbHZPGCbiV9uO9iPlgoZ56t9y","object":"chat.completion.chunk","created":1686796770,"model":"gpt-3.5-turbo-16k-0613","choices":[{"index":0,"delta":{"content":"。"},"finish_reason":null}]}
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据：{"id":"chatcmpl-7RXBSbHZPGCbiV9uO9iPlgoZ56t9y","object":"chat.completion.chunk","created":1686796770,"model":"gpt-3.5-turbo-16k-0613","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据：[DONE]
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI返回数据结束了
     * [OkHttp https://dgr.life/...] INFO com.unfbx.chatgpt.ConsoleEventSourceListenerV2 - OpenAI关闭sse连接...
     */
    @Test
    public void streamChatFunction() {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        ConsoleEventSourceListenerV2 eventSourceListener = new ConsoleEventSourceListenerV2(countDownLatch);

        //模型：GPT_3_5_TURBO_16K_0613
        Message message = Message.builder().role(Message.Role.USER).content("给我输出一个长度为2的中文词语，并解释下词语对应物品的用途").build();
        //属性一
        JSONObject wordLength = new JSONObject();
        wordLength.putOpt("type", "number");
        wordLength.putOpt("description", "词语的长度");
        //属性二
        JSONObject language = new JSONObject();
        language.putOpt("type", "string");
        language.putOpt("enum", Arrays.asList("zh", "en"));
        language.putOpt("description", "语言类型，例如：zh代表中文、en代表英语");
        //参数
        JSONObject properties = new JSONObject();
        properties.putOpt("wordLength", wordLength);
        properties.putOpt("language", language);
        Parameters parameters = Parameters.builder()
                .type("object")
                .properties(properties)
                .required(Arrays.asList("wordLength")).build();
        Functions functions = Functions.builder()
                .name("getOneWord")
                .description("获取一个指定长度和语言类型的词语")
                .parameters(parameters)
                .build();

        ChatCompletion chatCompletion = ChatCompletion
                .builder()
                .messages(Arrays.asList(message))
                .functions(Arrays.asList(functions))
                .functionCall("auto")
                .model(ChatCompletion.Model.GPT_3_5_TURBO_16K_0613.getName())
                .build();
        openAiStreamClient.streamChatCompletion(chatCompletion, eventSourceListener);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String args = eventSourceListener.getArgs();
        log.info("构造的方法参数：{}", args);
        WordParam wordParam = JSONUtil.toBean(args, WordParam.class);
        String oneWord = getOneWord(wordParam);

        FunctionCall functionCall = FunctionCall.builder()
                .arguments(args)
                .name("getOneWord")
                .build();
        Message message2 = Message.builder().role(Message.Role.ASSISTANT).content("方法参数").functionCall(functionCall).build();
        String content
                = "{ " +
                "\"wordLength\": \"3\", " +
                "\"language\": \"zh\", " +
                "\"word\": \"" + oneWord + "\"," +
                "\"用途\": [\"直接吃\", \"做沙拉\", \"售卖\"]" +
                "}";
        Message message3 = Message.builder().role(Message.Role.FUNCTION).name("getOneWord").content(content).build();
        List<Message> messageList = Arrays.asList(message, message2, message3);
        ChatCompletion chatCompletionV2 = ChatCompletion
                .builder()
                .messages(messageList)
                .model(ChatCompletion.Model.GPT_3_5_TURBO_16K_0613.getName())
                .build();
        CountDownLatch countDownLatch1 = new CountDownLatch(1);
        openAiStreamClient.streamChatCompletion(chatCompletionV2, new ConsoleEventSourceListenerV2(countDownLatch));
        try {
            countDownLatch1.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取一个词语
     * @param wordParam
     * @return
     */
    public String getOneWord(WordParam wordParam) {

        List<String> zh = Arrays.asList("大香蕉", "哈密瓜", "苹果");
        List<String> en = Arrays.asList("apple", "banana", "cantaloupe");
        if (wordParam.getLanguage().equals("zh")) {
            for (String e : zh) {
                if (e.length() == wordParam.getWordLength()) {
                    return e;
                }
            }
        }
        if (wordParam.getLanguage().equals("en")) {
            for (String e : en) {
                if (e.length() == wordParam.getWordLength()) {
                    return e;
                }
            }
        }
        return "西瓜";
    }

    @Test
    public void testInput() {
        System.out.println(getOneWord(WordParam.builder().wordLength(2).language("zh").build()));
    }

    @Data
    @Builder
    static class WordParam {
        private int wordLength;
        @Builder.Default
        private String language = "zh";
    }


    @Test
    public void chatFunctionTest() {
        //模型：GPT_3_5_TURBO_16K_0613
        Message message = Message.builder().role(Message.Role.USER).content("查找华为技术有限公司最新申请的20篇专利").build();
        //属性一
        JSONObject wordLength = new JSONObject();
        wordLength.putOpt("type", "number");
        wordLength.putOpt("description", "列表结果的页数");

        JSONObject limit = new JSONObject();
        limit.putOpt("type", "number");
        limit.putOpt("description", "列表结果每页有几条专利");
        //属性二
        JSONObject q = new JSONObject();
        q.putOpt("type", "string");
        q.putOpt("description", "检索语句，比如查询百度公司的专利，语句为:ANCS\"百度公司\"");

        JSONObject sort = new JSONObject();
        sort.putOpt("type", "string");
        sort.putOpt("enum", Arrays.asList("sdesc", "desc"));
        sort.putOpt("description", "获取专利的排序方式，比如按最新申请排序（desc）、按相关度最高排序（sdesc）");

        JSONObject playbook = new JSONObject();
        playbook.putOpt("type", "string");
        playbook.putOpt("enum", Arrays.asList("SmartSearch", "NoveltySearch"));
        playbook.putOpt("description", "检索类型，比如简单检索语句的为简单检索（smartSearch），检索语句超过100字符的为语义检索（NoveltySearch）");

        JSONObject type = new JSONObject();
        type.putOpt("type", "string");
        type.putOpt("enum", Arrays.asList("query"));
        type.putOpt("description", "默认检索方式：query");
        //参数
        JSONObject properties = new JSONObject();
        properties.putOpt("page", wordLength);
        properties.putOpt("limit", limit);
        properties.putOpt("q", q);
        properties.putOpt("_type", type);
        properties.putOpt("playbook", playbook);
        properties.putOpt("sort", sort);
        Parameters parameters = Parameters.builder()
                .type("object")
                .properties(properties)
                .required(Arrays.asList("sort","page","q","playbook","_type")).build();
        Functions functions = Functions.builder()
                .name("obtainPatentList")
                .description("通过查询语句和查询数量等检索获取一批专利信息的结果")
                .parameters(parameters)
                .build();

        ChatCompletion chatCompletion = ChatCompletion
                .builder()
                .messages(Collections.singletonList(message))
                .functions(Collections.singletonList(functions))
                .functionCall("auto")
                .model(ChatCompletion.Model.GPT_3_5_TURBO_16K_0613.getName())
                .build();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ConsoleEventSourceListenerV2 eventSourceListener = new ConsoleEventSourceListenerV2(countDownLatch);

        openAiStreamClient.streamChatCompletion(chatCompletion, eventSourceListener);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String args = eventSourceListener.getArgs();
        System.out.printf(args);

        String apiResponse = "{\"status\":true,\"error_code\":0,\"data\":{\"doc\":\"ANCS:\\\"华为技术有限公司\\\"\",\"doc_s\":{\"type\":\"BOOL\",\"logic\":\"AND\",\"items\":[{\"type\":\"FIELD\",\"logic\":\"AND\",\"field\":\"ANCS\",\"items\":[{\"type\":\"KEYWORD\",\"logic\":\"AND\",\"value\":\"华为技术有限公司\"}]}]},\"finally_query\":\"ANCS:华为技术有限公司\",\"page\":1,\"limit\":20,\"has_previous\":false,\"has_next\":true,\"total\":4310,\"records\":[{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"b3166506-40b5-42b9-95d2-1cfc54de13f1\",\"PN\":\"CN113826342B\",\"TITLE_LANG\":\"CN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"一种通信方法及相关设备\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"一种通信方法、终端设备的技术，应用在网络领域，能够解决LTE系统上行容量低等问题\",\"CONTENT_TYPE\":\"TECHNICAL_TITLE\",\"CONTENT_LANG\":\"CN\",\"CONTENT_TRAN\":\"一种通信方法、终端设备的技术，应用在网络领域，能够解决LTE系统上行容量低等问题\",\"CONTENT_TRAN_LANG\":\"CN\",\"TECHNICAL_TITLE\":\"一种通信方法、终端设备的技术，应用在网络领域，能够解决LTE系统上行容量低等问题\",\"TECHNICAL_TITLE_LANG\":\"CN\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"73aba826-327f-4029-b42c-44641eea97d2\",\"PN\":\"CN111722333B\",\"TITLE_LANG\":\"CN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"一种光缆连接装置\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"一种光缆连接、旋转连接的技术，应用在光学、光导、光学元件等方向，能够解决卡接可靠性不高、易损坏、不能盖子02和壳体01锁止等问题\",\"CONTENT_TYPE\":\"TECHNICAL_TITLE\",\"CONTENT_LANG\":\"CN\",\"CONTENT_TRAN\":\"一种光缆连接、旋转连接的技术，应用在光学、光导、光学元件等方向，能够解决卡接可靠性不高、易损坏、不能盖子02和壳体01锁止等问题\",\"CONTENT_TRAN_LANG\":\"CN\",\"TECHNICAL_TITLE\":\"一种光缆连接、旋转连接的技术，应用在光学、光导、光学元件等方向，能够解决卡接可靠性不高、易损坏、不能盖子02和壳体01锁止等问题\",\"TECHNICAL_TITLE_LANG\":\"CN\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"41220fcf-1fc4-4dfc-b8e9-6f75fd67a9e7\",\"PN\":\"EP4060916A1\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Frequency adjustment method and communication apparatus\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>Embodiments of this application relate to the communication field, and provide a frequency adjustment method and a communication apparatus. The method includes: A communication apparatus sends a first downlink signal to a terminal device via a first transmission reception point TRP; receives an uplink signal from the terminal device via the first TRP, where a receive frequency of the uplink signal at the first TRP is a first receive frequency; receives an uplink signal from the terminal device via a second TRP, where a receive frequency of the uplink signal at the second TRP is a second receive frequency; sends a second downlink signal to the terminal device via the second TRP; and sends a third downlink signal to the terminal device via the first TRP, where a difference obtained by subtracting a transmit frequency of the third downlink signal at the first TRP from a transmit frequency of the second downlink signal at the second TRP is equal to △f/k, △f is equal to a difference obtained by subtracting the second receive frequency from the first receive frequency, and k is a ratio of an uplink center frequency to a downlink center frequency.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"035ffe77-7cd1-4fe7-97d1-9c21f943d1cb\",\"PN\":\"US11342654B2\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Base station antenna, switch, and base station device\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>This application provides examples of a base station antenna, a switch, and a base station device. A connection status between an output port and an input port of a horizontal-dimensional feeding network is changed by using a switch of the horizontal-dimensional feeding network. In different connection statuses, quantities of input ports that are connected to a plurality of output ports of the horizontal-dimensional feeding network are different. The input port of the horizontal-dimensional feeding network is in communication with an antenna port to form a transceiver channel. In this case, a quantity of transceiver channels, of the horizontal-dimensional feeding network, formed in each connection status is different. Therefore, the quantity of transceiver channels supported by the base station device can be changed by using the base station antenna without a need of replacing the base station antenna.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"6705fe69-3b9a-4515-9fd3-ea5da999fc33\",\"PN\":\"EP3910793A1\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Buffer circuit, frequency divider circuit, and communication device\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>A buffer circuit (10), a frequency dividing circuit (1), and a communications device are disclosed, to cancel phase noise generated by the frequency dividing circuit (1), and improve phase noise performance of an output signal. The buffer circuit (10) includes a buffer (100), a first control circuit (200), and a second control circuit (300). The buffer (100) is coupled to a frequency divider (20), and the buffer (100) is configured to receive a first signal output by the frequency divider (20), and output a fourth signal by using an output terminal (400) of the buffer circuit (10) when driven by the first signal, where the first signal is obtained by the frequency divider (20) by performing frequency division on a group of differential signals, and the differential signals include a second signal and a third signal. The first control circuit (200) is configured to perform delay control on a rising edge of the fourth signal based on the second signal. The second control circuit (300) is configured to perform delay control on a falling edge of the fourth signal based on the third signal.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"03ce5bbc-80b2-4b50-a849-a4c39d39d462\",\"PN\":\"US20210329399A1\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Audio signal processing method and apparatus\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>In an audio signal processing method, a processing device obtains a current position relationship between a sound source and a listener. The processing device then obtains a current audio rendering function based on the current position relationship. When the current position relationship is different from a stored previous position relationship, the processing device adjusts an initial gain of the current audio rendering function based on the current position relationship and the previous position relationship, to obtain an adjusted gain of the current audio rendering function. The processing device then obtains an adjusted audio rendering function based on the current audio rendering function and the adjusted gain, and generates a current output signal based on a current input signal and the adjusted audio rendering function.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"5ea5277a-203e-46fa-83e8-62d3f8a9ff9f\",\"PN\":\"US11140037B2\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Communication method, network device, and system\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>This application provides a communication method, a network device, and a system. The method includes: receiving, by a first network device that manages a network slice subnet instance, requirement description information of the network slice subnet instance from a second network device that manages a network slice instance, where the requirement description information of the network slice subnet instance is used to indicate a service requirement for the network slice subnet instance, and the network slice instance includes the network slice subnet instance; and creating or configuring, by the first network device, the network slice subnet instance based on configuration information of the network slice subnet instance, where the configuration information of the network slice subnet instance is determined based on the requirement description information of the network slice subnet instance. The communication method in the embodiments of this application can improve management efficiency of a network slice instance.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"d9d358b4-41d5-42a2-bac4-2e0fcbe2954b\",\"PN\":\"CN113348697A\",\"TITLE_LANG\":\"CN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"无线通信方法、装置及系统\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"一种无线通信方法、装置及系统的技术，应用在无线通信、电气元件等方向\",\"CONTENT_TYPE\":\"TECHNICAL_TITLE\",\"CONTENT_LANG\":\"CN\",\"CONTENT_TRAN\":\"一种无线通信方法、装置及系统的技术，应用在无线通信、电气元件等方向\",\"CONTENT_TRAN_LANG\":\"CN\",\"TECHNICAL_TITLE\":\"一种无线通信方法、装置及系统的技术，应用在无线通信、电气元件等方向\",\"TECHNICAL_TITLE_LANG\":\"CN\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"b6abe397-955d-4901-a56f-2a74ab552efd\",\"PN\":\"US11108531B2\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Method and apparatus for setting symbol\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>Embodiments of this application provide a method and an apparatus for setting a symbol in a communications system that uses a plurality of subcarrier spacings. The method includes: obtaining, by a terminal, a length of a reference blank symbol, where the length of the reference blank symbol is associated with a first subcarrier spacing, and the first subcarrier spacing is a minimum subcarrier spacing in the plurality of subcarrier spacings; and setting, by the terminal based on the length of the reference blank symbol and time domain information of the reference blank symbol, a blank symbol for a subcarrier corresponding to a second subcarrier spacing in the plurality of subcarrier spacings. According to the method and apparatus for setting a symbol provided in the embodiments of this application, when setting, based on the length of the reference symbol, a blank symbol in a subframe corresponding to a subcarrier used by the terminal, the terminal may set one or more complete symbols as blank symbols. This avoids a case in which a symbol cannot work normally because a part of the symbol is set as a blank symbol, thereby improving spectral efficiency of the system.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"022737e2-3637-467b-94ff-91cf9280f8ff\",\"PN\":\"WO2021163974A1\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Communication method and apparatus\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>Provided are a communication method and apparatus, which relate to the technical field of communications, and can enable a terminal to acquire a discovery parameter in a device-to-device (D2D) discovery process when multiple management and control network elements are deployed in each PLMN. The solution involves: a first management and control network element assigning a PDUID to a first terminal; the first management and control network element sending first information to a first storage network element, wherein the first information comprises the PDUID of the first terminal and an identifier of the first management and control network element, the first information is used for the first storage network element to store the correlation between the PDUID of the first terminal and the identifier of the first management and control network element, the first management and control network element stores a discovery parameter of the first terminal, and the discovery parameter is used for D2D discovery. The embodiments of the present application are used for D2D discovery.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"0238505f-3506-4580-ac64-fd0baa81f523\",\"PN\":\"US11023176B2\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Storage interface, timing control method, and storage system\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>The storage interface includes a first programmable input/output unit configured to perform phase inversion on a clock signal that is output by the master controller, and output the phase-inverted clock signal to the storage device. The storage interface includes a second programmable input/output unit configured to delay a data signal that is output by the master controller, and output the delayed data signal to the storage device, where the delayed data signal is delayed by a time ΔT relative to the clock signal that is output by the master controller, and T<sub>CLK</sub>/2−ΔT≥T<sub>ISU </sub>and ΔT≥T<sub>IH</sub>, where T<sub>CLK </sub>represents a period of the clock signal, T<sub>ISU </sub>represents a shortest input setup time required by the storage device in each of different data rate modes, and T<sub>IH </sub>represents a shortest input hold time employed by the storage device in each of different data rate modes.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"d652b76c-2533-4e9c-9a93-873cb225c4a8\",\"PN\":\"CN112825153A\",\"TITLE_LANG\":\"CN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"神经网络系统中数据处理的方法、神经网络系统\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"一种神经网络、数据处理的技术，应用在神经网络领域\",\"CONTENT_TYPE\":\"TECHNICAL_TITLE\",\"CONTENT_LANG\":\"CN\",\"CONTENT_TRAN\":\"一种神经网络、数据处理的技术，应用在神经网络领域\",\"CONTENT_TRAN_LANG\":\"CN\",\"TECHNICAL_TITLE\":\"一种神经网络、数据处理的技术，应用在神经网络领域\",\"TECHNICAL_TITLE_LANG\":\"CN\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"750432b3-7ab7-49af-a5f0-bdca73d32c9c\",\"PN\":\"EP3823382A1\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Communication method and apparatus\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>This application discloses a communications method and apparatus. The method includes: receiving, by a terminal device, PUCCH resource configuration indication information from a network device, where the PUCCH resource configuration indication information is associated with one or more of the following parameters: a downlink channel resource parameter, a demodulation reference signal port-related parameter, and a transport block parameter. A corresponding apparatus is further disclosed. According to the solution in this application, the PUCCH resource configuration indication information is associated with one or more parameters of the downlink channel resource, the demodulation reference signal port-related parameter, and the transport block parameter. When the terminal device sends uplink control information by using a PUCCH resource, a PUCCH resource conflict can be avoided, thereby ensuring transmission reliability.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"f59523c0-71d6-42b7-9e17-3de06c123359\",\"PN\":\"BRPI1007309B1\",\"TITLE_LANG\":\"PT\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"MÉTODO E APARELHO DE ALOCAÇÃO DE RECURSOS, E MÉTODO E APARELHO PARA PROCESSAMENTO DE INFORMAÇÃO DE CONFIRMAÇÃO\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>método e aparelho para a alocação de recursos e o processamento de uma informação de confirmação. a presente invenção refere-se a um método e um aparelho para a alocação de recursos de canal de reconhecimento (ack) e não reconhecimento (nack) e processamento de informação de confirmação são mostrados. o método inclui: o lado de rede determina uma área de cannal físico dentre múltiplas áreas de canal físico a ser usada por um canal de ack / nack, e notifica a área de canal físico determinada para um equipamento de usuário (ue), de modo a permitir que o ue determine um canal para o recebimento ou envio da informação de ack/nack na área de canal físico determinada, de acordo com uma regra de mapeamento. mais ainda,o lado de rede pode enviar ou receber uma informação de ack/nack. na área de canal físico que inclui o canal de ack/nack. o método e o aparelho melhoram a relação de utilização e a flexibilidade do canla de ack/nack,e reduzem a probalidade de um conflito gerado pelo canal de ack/nack.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"PT\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"fcb755f8-ae43-487f-9873-27ea67424421\",\"PN\":\"US20210058119A1\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Channel estimation method and apparatus\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>This application discloses a channel estimation method and apparatus, and relates to the field of communications technologies, to help reduce indication overheads. The method may include: generating and sending indication information. The indication information is used to indicate K N-dimensional spatial-domain component vectors, L M-dimensional frequency-domain component vectors, and a weight matrix, to indicate to construct an M×N-dimensional spatial-frequency matrix, or an M×N or N×M spatial-frequency matrix.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"f22bcf49-f366-41ca-9c05-f971da090cdc\",\"PN\":\"EP3104646B1\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Data transmission method, system and related device\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>Embodiments of the present invention disclose a data transmission method and system, and a related apparatus. The method in the embodiments of the present invention includes: when a network-side data transmission apparatus detects that a terminal is handed over from being served by a source base station to being served by a target base station, determining whether the source base station and the target base station have a wireless network transmission optimization WNTO technical capability; and completing transmission of uplink data or downlink data according to each determined WNTO technical capability of the source base station and the target base station, which effectively improves data transmission efficiency in a wireless network.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"d0b080b6-b929-4aeb-bda6-f6423320a9cc\",\"PN\":\"CN108290521B\",\"TITLE_LANG\":\"CN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"一种影像信息处理方法及增强现实AR设备\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"一种信息处理方法、设备的技术，应用在图像增强、图像数据处理、图像分析等方向，能够解决行车安全影响、很难获知环境动态、人眼视力有限等问题，达到提升行车安全性的效果\",\"CONTENT_TYPE\":\"TECHNICAL_TITLE\",\"CONTENT_LANG\":\"CN\",\"CONTENT_TRAN\":\"一种信息处理方法、设备的技术，应用在图像增强、图像数据处理、图像分析等方向，能够解决行车安全影响、很难获知环境动态、人眼视力有限等问题，达到提升行车安全性的效果\",\"CONTENT_TRAN_LANG\":\"CN\",\"TECHNICAL_TITLE\":\"一种信息处理方法、设备的技术，应用在图像增强、图像数据处理、图像分析等方向，能够解决行车安全影响、很难获知环境动态、人眼视力有限等问题，达到提升行车安全性的效果\",\"TECHNICAL_TITLE_LANG\":\"CN\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"99560101-f8eb-4734-b627-8d7780b98ee6\",\"PN\":\"US20200275437A1\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Uplink Control Channel Resource Configuration Method And Apparatus\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>Embodiments of the present disclosure relate to the field of communications technologies, and provide an uplink control channel resource configuration method and an apparatus, to improve utilization of a frequency domain resource of an uplink channel. The method includes: determining, by a base station, resource configuration information, and sending, by the base station, the resource configuration information to UE, where the resource configuration information includes a first hop resource of a first uplink control channel (PUCCH), a frequency domain resource offset of the first PUCCH, and/or a bandwidth of a bandwidth part (BWP) used by the user equipment UE, and the first PUCCH is a PUCCH of the UE.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"ff8c91ec-8c48-42c3-bef4-057af9969576\",\"PN\":\"WO2020114508A1\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Video encoding/decoding method and apparatus\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>A video decoding method and apparatus, and a corresponding encoding/decoding device, for improving the encoding/decoding performance on a transform unit to a certain extent. The method comprises: obtaining a coding block flags of N-1 transform tree child nodes in N transform tree child nodes, N being an integer greater than 1; determining, according to the values of the coding block flags of the N-1 transform tree child nodes, the value of the coding block flag of the transform tree child node in the N transform tree child nodes other than the N-1 transform tree child nodes; and obtaining an image block indicated by the decoded current transform tree node according to the coding block flags of the N transform tree child nodes.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"},{\"SOLUTION_TYPE\":\"PATENT\",\"SOLUTION_ID\":\"48af55c9-cdd8-4723-b062-cc7c971a91ac\",\"PN\":\"WO2020088696A1\",\"TITLE_LANG\":\"EN\",\"TITLE_TRAN_LANG\":\"\",\"TITLE\":\"Packet forwarding method, packet transmission device, and packet reception device\",\"TITLE_TRAN\":\"\",\"CONTENT\":\"<div p='0' i='0'>Provided are a packet forwarding method, a packet transmission device, and a packet reception device. The method comprises: for a first node on the control plane, configuring a first identifier and a first IPv6 address of a first VPN, said first identifier corresponding to the first IPv6 address; the first node places the first identifier and the first IPv6 address in a first indication message and transmits same to a second node; the second node first determines, according to the first VPN information locally configured and the first identifier in the first indication message, a second identifier in the second node that satisfies a pre-set correspondence relation with the first identifier, and then establishes a correspondence relation between the first IPv6 address and the second identifier. On the forwarding plane, the first node encapsulates, according to the first IPv6 address, the multicast data packets that belong to the first VPN, obtains a BIER packet to be forwarded, and transmits same. The encapsulation and packet forwarding performance are enhanced.</div>\",\"CONTENT_TYPE\":\"ABST\",\"CONTENT_LANG\":\"EN\",\"CONTENT_TRAN\":\"\",\"CONTENT_TRAN_LANG\":\"\",\"TECHNICAL_TITLE\":\"\",\"TECHNICAL_TITLE_LANG\":\"\"}],\"remind\":false,\"doc_lang\":\"cn\",\"pum_facet\":false}}";
        Message messageResponse = Message.builder().role(Message.Role.FUNCTION).name("obtainPatentList").content(apiResponse).build();
        Message messageRequest = Message.builder().role(Message.Role.ASSISTANT).content("").functionCall(new FunctionCall("obtainPatentList", args)).build();
        chatCompletion.setMessages(Arrays.asList(message, messageRequest, messageResponse));


        CountDownLatch countDownLatch2 = new CountDownLatch(1);
        ConsoleEventSourceListenerV2 eventSourceListener2 = new ConsoleEventSourceListenerV2(countDownLatch2);

        openAiStreamClient.streamChatCompletion(chatCompletion, eventSourceListener2);

        try {
            countDownLatch2.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        args = eventSourceListener2.getArgs();
        System.out.printf(args);

//        log.info("构造的方法值：{}", chatChoice.getMessage().getFunctionCall());
//        log.info("构造的方法名称：{}", chatChoice.getMessage().getFunctionCall().getName());
//        log.info("构造的方法参数：{}", chatChoice.getMessage().getFunctionCall().getArguments());
    }
}
