package org.phgb;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;

public class Main {

    private static final String KEY = UUID.randomUUID().toString();

    public static void main(String[] args) {
        ScopedValue<String> scopedValue = ScopedValue.newInstance();

        var task = getScopedValueCallable(scopedValue);
        var taskAsRunnable = toRunnable(task);

        // bounded in the same thread
        ScopedValue.where(scopedValue, KEY)
                .run(taskAsRunnable);

        // unbounded in a new thread
        var thread = Thread.ofPlatform()
                .name("new platform thread")
                .unstarted(taskAsRunnable);
        ScopedValue.where(scopedValue, KEY)
                .run(thread::start);

        // bounded in a StructuredTaskScope
        ScopedValue.where(scopedValue, KEY)
                .run(() -> {
                    try (var scope = new StructuredTaskScope<String>()) {
                        scope.fork(task);
                        scope.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static Callable<String> getScopedValueCallable(ScopedValue<String> scopedValue) {
        return () -> {
            System.out.println("Thread.currentThread().getName() = " + Thread.currentThread().getName());
            if (scopedValue.isBound()) {
                System.out.println("scopedValue.get() = " + scopedValue.get());
                return scopedValue.get();
            } else {
                System.out.println("value not bounded");
                return "not bounded";
            }
        };
    }

    private static Runnable toRunnable(Callable<String> task) {
        return () -> {
            try {
                task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}