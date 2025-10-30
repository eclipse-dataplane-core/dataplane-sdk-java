package org.eclipse.dataplane.domain;

import java.util.NoSuchElementException;
import java.util.function.Function;

public abstract class Result<C> {

    public static Result<Void> success() {
        return success(null);
    }

    public static <C> Result<C> success(C content) {
        return new Success<>(content);
    }

    public static <R> Result<R> failure(Exception e){ return new Failure<>(e); }

    public static <R> Result<R> attempt(ExceptionThrowingSupplier<R> resultSupplier) {
        try {
            var resultValue = resultSupplier.get();
            return Result.success(resultValue);
        } catch (Exception exception){
            return Result.failure(exception);
        }
    }

    public abstract C getContent();

    public abstract C orElseThrow() throws Exception;

    public abstract <X extends Throwable> C orElseThrow(Function<Exception, X> exceptionSupplier) throws X;

    public abstract <T> Result<T> map(ExceptionThrowingFunction<C,T> transformValue);

    public abstract <T> Result<T> compose(ExceptionThrowingFunction<C, Result<T>> transformValue);

    public boolean succeeded() {
        return this instanceof Result.Success<C>;
    }

    public boolean failed() {
        return this instanceof Result.Failure<C>;
    }

    private static class Success<C> extends Result<C> {

        private final C content;

        public Success(C content) {
            this.content = content;
        }

        @Override
        public C getContent() {
            return content;
        }

        @Override
        public C orElseThrow() throws Exception {
            return content;
        }

        @Override
        public <X extends Throwable> C orElseThrow(Function<Exception, X> exceptionSupplier) throws X {
            return content;
        }

        @Override
        public <T> Result<T> map(ExceptionThrowingFunction<C, T> transformValue) {
            return Result.attempt(() -> transformValue.apply(this.content));
        }

        @Override
        public <T> Result<T> compose(ExceptionThrowingFunction<C, Result<T>> transformValue) {
            try {
                return transformValue.apply(this.content);
            } catch(Exception e) {
                return Result.failure(e);
            }
        }
    }

    private static class Failure<C> extends Result<C> {

        private final Exception exception;
        private Failure(Exception exception) {
            this.exception = exception;
        }

        @Override
        public C getContent() {
            throw new NoSuchElementException("The result has no content because it's failed", exception);
        }

        @Override
        public C orElseThrow() throws Exception {
            throw this.exception;
        }

        @Override
        public <X extends Throwable> C orElseThrow(Function<Exception, X> exceptionSupplier) throws X {
            throw exceptionSupplier.apply(this.exception);
        }

        @Override
        public <T> Result<T> map(ExceptionThrowingFunction<C, T> transformValue) {
            return Result.failure(this.exception);
        }

        @Override
        public <T> Result<T> compose(ExceptionThrowingFunction<C, Result<T>> transformValue) {
            return Result.failure(this.exception);
        }
    }

    @FunctionalInterface
    public interface ExceptionThrowingFunction<T,R> {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface ExceptionThrowingSupplier<T> {
        T get() throws Exception;
    }
}
