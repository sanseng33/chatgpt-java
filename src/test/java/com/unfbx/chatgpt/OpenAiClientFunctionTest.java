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
        Message message = Message.builder().role(Message.Role.USER).content("查找华为技术有限公司最新申请的20篇专利,获取专利的摘要信息，将信息总结成几个技术方向。").build();
        Message system = Message.builder().role(Message.Role.SYSTEM).content("如果用户的请求是模棱两可的，那么不要猜测函数中应该插入什么值，而是要求说明。").build();

        //构造 获取专利函数
        Parameters parameters = Parameters.builder()
                .type("object")
                .properties(transform(new SrpRequest()))
                .required(Arrays.asList("sort","page","q","playbook","_type")).build();
        Parameters outParameters = Parameters.builder()
                .type("object")
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
                .functionCall("auto")
                .model(ChatCompletion.Model.GPT_3_5_TURBO_16K_0613.getName())
                .build();

        String jsonStr = JSONUtil.toJsonStr(chatCompletion);
        System.out.print(jsonStr);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        ConsoleEventSourceListenerV2 eventSourceListener = new ConsoleEventSourceListenerV2(countDownLatch);
        openAiStreamClient.streamChatCompletion(chatCompletion, eventSourceListener);
//        openAiClient.chatCompletion(chatCompletion);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String args = eventSourceListener.getArgs();
        System.out.print(args);

        //拿生成的参数调用相应api
        String apiResponse = obtainHttpData(args);


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

    private String obtainHttpData(String arg) {
        try {
            String url =  "http://localhost:8080/eureka/search/srp";
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

            if (response.isSuccessful()) {
                String body = response.body().string();
                Map map = SimpleJsonMapper.readValue(body, Map.class);
                Object data = ((Map)map.get("data")).get("records");
                return SimpleJsonMapper.writeValue(data);
            }

        } catch (Exception ex) {
            log.error("obtainHttpData call api error.request", ex);
        }
        return null;
    }
}
