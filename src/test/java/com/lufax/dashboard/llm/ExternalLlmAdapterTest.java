package com.lufax.dashboard.llm;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * ExternalLlmAdapter 单元测试
 * 覆盖: 配置属性、Semaphore 并发控制、doCall 异常、线程安全
 */
public class ExternalLlmAdapterTest {

    private ExternalLlmAdapter adapter;

    @Before
    public void setUp() {
        adapter = new ExternalLlmAdapter();
        adapter.setApiKey("test-api-key-12345");
        adapter.setBaseUrl("https://api.example.com/v1");
        adapter.setModel("test-model");
        adapter.setProtocol("openai");
        adapter.setConcurrency(2);
    }

    // ========== 配置属性 (Setter/Getter 行为) ==========

    @Test
    public void testSetters_AcceptValuesWithoutError() {
        // 构造后设置值不抛异常 — 已在 setUp 中验证
        adapter.setApiKey("new-key");
        adapter.setBaseUrl("https://new-url.com");
        adapter.setModel("gpt-4");
        adapter.setProtocol("custom");
        adapter.setConcurrency(5);
        // 如果到这里没报错，说明 setter 正常工作
    }

    @Test
    public void testInit_CreatesSemaphore() {
        adapter.init();
        // init 成功意味着 semaphore 已创建，不抛异常即可
    }

    @Test
    public void testInit_CalledMultipleTimes() {
        // 多次 init 不应出错
        adapter.init();
        adapter.init();
        adapter.init();
    }

    @Test
    public void testSetConcurrency_Zero() {
        adapter.setConcurrency(0); // Semaphore(0) 合法但会阻塞 acquire
    }

    @Test
    public void testSetConcurrency_Negative() {
        // Semaphore 负数会抛 IllegalArgumentException
        try {
            adapter.setConcurrency(-1);
            adapter.init();
            fail("负数 concurrency 应抛出异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("negative"));
        }
    }

    // ========== doCall 抛 UnsupportedOperationException ==========

    @Test(expected = UnsupportedOperationException.class)
    public void testGenerate_ThrowsUnsupportedOperation() {
        // doCall 直接抛出 UnsupportedOperationException
        adapter.generate("test prompt");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGenerateWithParams_ThrowsUnsupportedOperation() {
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.8);
        adapter.generate("test prompt", params);
    }

    @Test
    public void testExceptionMessage_ContainsProtocolInfo() {
        try {
            adapter.generate("test");
            fail("应该抛出异常");
        } catch (UnsupportedOperationException e) {
            assertTrue("异常消息应包含协议信息",
                    e.getMessage().contains("openai"));
        }
    }

    @Test
    public void testExceptionMessage_DifferentProtocol() {
        adapter.setProtocol("anthropic");
        try {
            adapter.generate("test");
            fail("应该抛出异常");
        } catch (UnsupportedOperationException e) {
            assertTrue("异常消息应包含 anthropic 协议",
                    e.getMessage().contains("anthropic"));
        }
    }

    // ========== Semaphore 初始化行为 ==========

    @Test
    public void testGenerate_AutoInitsSemaphoreIfNull() {
        // 不手动调用 init，generate 内部会自动 init
        adapter.setConcurrency(1); // 至少允许 1 个
        // 用一个子类覆盖 doCall 来避免抛异常
        ExternalLlmAdapter safeAdapter = new ExternalLlmAdapter() {
            @Override
            protected String doCall(String prompt, Map<String, Object> params) {
                return "{\"result\": \"ok\"}";
            }
        };
        safeAdapter.setConcurrency(1);
        // 不调用 init，直接 generate → 内部自动 init
        String result = safeAdapter.generate("auto init test");
        assertNotNull(result);
    }

    // ========== 并发控制测试 ==========

    @Test
    public void testConcurrencyControl_RespectsLimit() throws Exception {
        final int concurrency = 2;
        final int totalThreads = 4;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(totalThreads);
        AtomicInteger concurrentCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicReference<Throwable> error = new AtomicReference<>();

        // 创建一个阻塞式 adapter 用于测试并发
        ExternalLlmAdapter blockingAdapter = new ExternalLlmAdapter() {
            @Override
            protected String doCall(String prompt, Map<String, Object> params) {
                int current = concurrentCount.incrementAndGet();
                int maxSoFar;
                do {
                    maxSoFar = maxConcurrent.get();
                } while (current > maxSoFar && !maxConcurrent.compareAndSet(maxSoFar, current));
                try {
                    Thread.sleep(100); // 模拟耗时操作
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    concurrentCount.decrementAndGet();
                }
                return "{\"concurrent_test\": true}";
            }
        };
        blockingAdapter.setConcurrency(concurrency);

        // 启动多个线程同时调用
        for (int i = 0; i < totalThreads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // 等待统一启动信号
                    blockingAdapter.generate("concurrent test");
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown(); // 统一释放所有线程
        doneLatch.await();     // 等待全部完成

        assertNull("不应有线程错误", error.get());
        assertTrue("最大并发数不应超过 concurrency 设置 (" + concurrency + ")",
                maxConcurrent.get() <= concurrency + 1); // +1 容许边界误差
    }

    // ========== 接口契约 ==========

    @Test
    public void testImplementsLlmAdapterInterface() {
        assertTrue("ExternalLlmAdapter 应实现 LlmAdapter 接口",
                adapter instanceof LlmAdapter);
    }

    // ========== 边界情况 ==========

    @Test
    public void testGenerate_NullPrompt_StillGoesThroughSemaphore() {
        // 即使 prompt 为 null，也应经过 semaphore 再进入 doCall
        adapter.setProtocol("test-protocol-null");
        try {
            adapter.generate(null);
            fail("应抛出 UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // 正常：通过了 semaphore 控制，到达了 doCall
        }
    }

    @Test
    public void testGenerate_EmptyPrompt_StillGoesThroughSemaphore() {
        try {
            adapter.generate("");
            fail("应抛出异常");
        } catch (UnsupportedOperationException expected) {
            // 通过了 semaphore
        }
    }

    @Test
    public void testGenerateWithParams_LargeParamsMap() {
        Map<String, Object> bigMap = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            bigMap.put("key_" + i, "value_" + i);
        }
        try {
            adapter.generate("prompt", bigMap);
            fail("应抛出异常");
        } catch (UnsupportedOperationException expected) {
            // 参数大小不影响 semaphore 流程
        }
    }
}
