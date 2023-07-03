package com.unfbx.chatgpt;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.unfbx.chatgpt.aop.Description;
import com.unfbx.chatgpt.entity.PatentOriginalRequest;
import com.unfbx.chatgpt.entity.SrpRequest;
import com.unfbx.chatgpt.entity.SrpResponse;
import com.unfbx.chatgpt.entity.chat.*;
import com.unfbx.chatgpt.function.KeyRandomStrategy;
import com.unfbx.chatgpt.interceptor.DynamicKeyOpenAiAuthInterceptor;
import com.unfbx.chatgpt.interceptor.OpenAILogger;
import com.unfbx.chatgpt.interceptor.OpenAiResponseInterceptor;
import com.unfbx.chatgpt.sse.ConsoleEventSourceListener;
import com.unfbx.chatgpt.utils.SimpleJsonMapper;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
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
@Component
public class OpenAiClientFunctionTest {

    private OpenAiClient openAiClient;
    private RestTemplate resttemplate;
    private OpenAiStreamClient openAiStreamClient;


    @Before
    public void before() {
        //可以为null
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890));
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(new OpenAILogger());

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

    @Test
    public void chatFunctionTest() throws JsonProcessingException {
        //模型：GPT_3_5_TURBO_16K_0613
        Message message = Message.builder().role(Message.Role.USER)
                .content("查找华为技术有限公司最新申请的20篇专利,获取专利的摘要信息，将摘要信息总结成最多5个技术方向，只给出精简后的描述。").build();
        Message system = Message.builder().role(Message.Role.SYSTEM)
                .content("如果用户的请求是模棱两可的，那么不要随意猜测函数中应该插入什么值，而是要求用户进行说明。").build();

        //构造 获取专利函数
        Parameters parameters = Parameters.builder()
                .type("object")
                .properties(transform(new SrpRequest()))
                .required(Arrays.asList("sort","page","q","playbook","_type")).build();
        Parameters outParameters = Parameters.builder()
                .type("array")
                .properties(transform(new SrpResponse())).build();
        Functions functions = Functions.builder()
                .name("obtainPatentList")
//                .description("通过查询语句和查询数量等检索获取一批专利信息的结果")
                .description("通过查询语句和查询数量等检索获取一批专利的id")
                .parameters(parameters)
                .output(outParameters)
                .build();

        //构造 获取摘要函数
        Parameters abParameters = Parameters.builder()
                .type("object")
                .properties(transform(new PatentOriginalRequest()))
                .required(Arrays.asList("pids","fl","search_type")).build();
        Functions abFunctions = Functions.builder()
                .name("obtainPatentInfo")
                .description("通过专利id，获取专利的摘要、标题等专利信息")
                .parameters(abParameters)
                .build();

        //api調用获得请求参数
        ChatCompletion chatCompletion = ChatCompletion
                .builder()
                .messages(Arrays.asList(system, message))
                .functions(Arrays.asList(functions, abFunctions))
                // ”auto”  自动选择函数
                // ”none“  不选择函数
                // {"name": "<insert-function-name>"}  选择指定函数
                .functionCall("auto")
//                .functionCall(SimpleJsonMapper.readValue("{\"name\":\"obtainPatentInfo\"}", JSONObject.class))
                .model(ChatCompletion.Model.GPT_3_5_TURBO_16K_0613.getName())
                .build();

        log.info("当前请求体：{}", SimpleJsonMapper.writeValue(chatCompletion));

        //openai stream Completion
        ConsoleEventSourceListenerV2 eventSourceListener = getEventSourceListener2(chatCompletion);
        String funName = eventSourceListener.getFun();
        String args = eventSourceListener.getArgs();

        log.info("当前ai调用函数：{}", funName);
        log.info("当前ai返回参数：{}", args);

        //拿生成的参数调用相应api
        String apiResponse = obtainHttpData(funName, eventSourceListener.getArgs());

        //分析函数返回结果
        Message messageResponse = Message.builder().role(Message.Role.FUNCTION).name(funName).content(apiResponse).build();
        Message messageRequest = Message.builder().role(Message.Role.ASSISTANT).content("").functionCall(new FunctionCall(funName, args)).build();
        chatCompletion.setMessages(Arrays.asList(message, messageRequest, messageResponse));

        log.info("当前请求体：{}", SimpleJsonMapper.writeValue(chatCompletion));

        ConsoleEventSourceListenerV2 eventSourceListener2 = getEventSourceListener2(chatCompletion);

        funName = eventSourceListener2.getFun();
        args = eventSourceListener2.getArgs();
        if (Objects.nonNull(funName)) {
            String httpData = obtainHttpData(funName, args);
            messageResponse = Message.builder().role(Message.Role.FUNCTION).name(funName).content(httpData).build();
            messageRequest = Message.builder().role(Message.Role.ASSISTANT).content("").functionCall(new FunctionCall(funName, args)).build();
            chatCompletion.setMessages(Arrays.asList(message, messageRequest, messageResponse));

            log.info("当前请求体：{}", SimpleJsonMapper.writeValue(chatCompletion));

            ConsoleEventSourceListenerV2 eventSourceListener3 = getEventSourceListener2(chatCompletion);
            funName = eventSourceListener3.getFun();
            args = eventSourceListener3.getArgs();
            if (Objects.isNull(funName) || "".equals(funName)) {
                log.info("结论：{}", args);
            }
        }

//        log.info("结论：{}", args);

    }

    private ConsoleEventSourceListenerV2 getEventSourceListener2(ChatCompletion chatCompletion) {
        CountDownLatch countDownLatch2 = new CountDownLatch(1);
        ConsoleEventSourceListenerV2 eventSourceListener2 = new ConsoleEventSourceListenerV2(countDownLatch2);
        openAiStreamClient.streamChatCompletion(chatCompletion, eventSourceListener2);
        try {
            countDownLatch2.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return eventSourceListener2;
    }


    private JSONObject transform(Object request) {
        JSONObject json = new JSONObject();

        Field[] fields = request.getClass().getDeclaredFields();
        for (Field field : fields) {
            Description description = field.getAnnotation(Description.class);
            if (description != null) {
                JSONObject fieldDescription = new JSONObject();
                fieldDescription.putOpt("type", description.type());
                fieldDescription.putOpt("description", description.value());
                if (description.enumValues().length > 0) {
                    fieldDescription.putOpt("enum", description.enumValues());
                }
                if (description.type().equals("array")) {
                    JSONObject array = new JSONObject();
                    array.putOpt("type", description.arrayType());
                    fieldDescription.putOpt("items", array);
                }
                json.putOpt(description.name().equals("")?field.getName():description.name(), fieldDescription);
            }
        }

        return json;
    }

    private String obtainHttpData(String func, String arg) {
        try {
            String url =  func.equals("obtainPatentList")?
                    "http://localhost:8080/eureka/search/srp"
                    :"http://localhost:8080/eureka/patent/query/field";
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            OkHttpClient client = new OkHttpClient();

            RequestBody requestBody = RequestBody.create(JSON, arg);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("X-PatSnap-From", "s-core-eureka")
                    .addHeader("X-API-Version", "1.0")
                    .addHeader("X-PatSnap-Version", "v1")
                    .post(requestBody)
                    .build();

            Call call = client.newCall(request);
            Response response = call.execute();

            if (response.isSuccessful() && func.equals("obtainPatentList")) {
                String body = response.body().string();
                Map map = SimpleJsonMapper.readValue(body, Map.class);
                Object data = ((Map)map.get("data")).get("records");
                List<SrpResponse> responses = JSONUtil.toList(JSONUtil.parseArray(data), SrpResponse.class);
                return SimpleJsonMapper.writeValue(responses);
            }
            if (response.isSuccessful() && func.equals("obtainPatentInfo")) {
                String body = response.body().string();
                Map map = SimpleJsonMapper.readValue(body, Map.class);
                Object data = map.get("data");
                return SimpleJsonMapper.writeValue(data);
            }

        } catch (Exception ex) {
            log.error("obtainHttpData call api error.request", ex);
        }
        return null;
    }
}
