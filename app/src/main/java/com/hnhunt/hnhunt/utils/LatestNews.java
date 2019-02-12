package com.hnhunt.hnhunt.utils;

import android.content.Context;
import android.os.Handler;

import com.hnhunt.hnhunt.Consumer;
import com.hnhunt.hnhunt.HackerNewsAPI;
import com.hnhunt.hnhunt.HnNews;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class LatestNews {
    private static final int PAGE_SIZE = 3;
    private List<Long> data;
    HashSet<Integer> newsIds;
    private final Map<Long, HnNews> hnNewsHashMap;
    private volatile int lastIndex = 0;
    private static LatestNews latestNews;
    private LatestNews() {
        data = new ArrayList<>();
        newsIds = new HashSet<>();
        hnNewsHashMap = new ConcurrentHashMap<>();
    }
    public int size() {
        return lastIndex;
    }
    public static LatestNews getInstance() {
        if (latestNews != null) return latestNews;
        return latestNews = new LatestNews();
    }

    public int getLastUpdatedIndex() {
        return lastIndex;
    }
    public HnNews getHnNews(int position) {
        if (position >= lastIndex) throw new RuntimeException("Invalid index");
        return hnNewsHashMap.get(data.get(position));
    }
    public void addData(List<Long> news) {
        this.data = news;
    }

    public void resetData(List<Long> newData) {
    }

    public void refreshNextPage(Context context, Consumer<Void> callback, Consumer<Exception> exceptionConsumer) {
        final Exception[] lastException = new Exception[1];

        CountDownLatch countDownLatch = new CountDownLatch(PAGE_SIZE);
        for (int i = lastIndex; i < Math.min(lastIndex + PAGE_SIZE, data.size()); i++) {
            long hnId = data.get(i);
            HackerNewsAPI.getStory(context, hnId, (hn) -> {
                hnNewsHashMap.put(hnId, hn);
                countDownLatch.countDown();
            }, (ex) -> {
                lastException[0] = ex;
            });
        }

        new Thread(() -> {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                runOnUIThread(context, () -> exceptionConsumer.accept(lastException[0]));
            }
            if (lastException[0] == null) {
                runOnUIThread(context, () -> {
                    lastIndex += PAGE_SIZE;
                    callback.accept(null);
                });
            } else {
                runOnUIThread(context, () -> exceptionConsumer.accept(lastException[0]));
            }
        }).start();
    }

    private static void runOnUIThread(Context context, Runnable runnable) {
        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(context.getMainLooper());
        mainHandler.post(runnable);
    }
}
