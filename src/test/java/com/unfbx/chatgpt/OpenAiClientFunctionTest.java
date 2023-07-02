package com.unfbx.chatgpt;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.google.common.collect.Maps;
import com.unfbx.chatgpt.aop.Description;
import com.unfbx.chatgpt.entity.SrpRequest;
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

    @Test
    public void chatFunctionTest() throws JsonProcessingException {
        //模型：GPT_3_5_TURBO_16K_0613
        Message message = Message.builder().role(Message.Role.USER).content("查找华为技术有限公司最新申请的20篇专利,总结成几个技术方向。").build();

        //构造函数1
        Parameters parameters = Parameters.builder()
                .type("object")
                .properties(transform(new SrpRequest()))
                .required(Arrays.asList("sort","page","q","playbook","_type")).build();
        Functions functions = Functions.builder()
                .name("obtainPatentList")
                .description("通过查询语句和查询数量等检索获取一批专利信息的结果")
                .parameters(parameters)
                .build();

        //构造函数2


        //api調用获得请求参数
        ChatCompletion chatCompletion = ChatCompletion
                .builder()
                .messages(Collections.singletonList(message))
                .functions(Collections.singletonList(functions))
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



    private JSONObject transform(SrpRequest srpRequest) {
        JSONObject json = new JSONObject();

        Field[] fields = SrpRequest.class.getDeclaredFields();
        for (Field field : fields) {
            Description description = field.getAnnotation(Description.class);
            if (description != null) {
                JSONObject fieldDescription = new JSONObject();
                fieldDescription.put("type", description.type());
                fieldDescription.put("description", description.value());
                if (description.enumValues().length > 0) {
                    fieldDescription.put("enum", description.enumValues());
                }
                json.put(field.getName(), fieldDescription);
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
                    .post(requestBody)
                    .build();

            Call call = client.newCall(request);
            Response response = call.execute();

            if (response.isSuccessful()) {
                Object data = SimpleJsonMapper.readValue(SimpleJsonMapper.writeValue(response.body()), Map.class).get("data");
                return SimpleJsonMapper.writeValue(data);
            }

        } catch (Exception ex) {
            log.error("obtainHttpData call api error.request", ex);
        }
        return null;
    }
}
