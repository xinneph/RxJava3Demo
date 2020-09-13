import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class ObservableSwitchMapDemo {
    public static void main(String[] args) {
        ConfigurationFetcher configurationFetcher = new ConfigurationFetcher();
        ConfigurationLoader configurationLoader = new ConfigurationLoader(configurationFetcher);
        TestObserver<String> observer = configurationLoader.configuration.test();
        observer.assertNoErrors();
        System.out.println(observer.values().size());
        System.out.println(observer.values());
    }

    private static final String sampleJson = "\"isMessagingEnabled\": true}";

    static class ConfigurationLoader {
        private BehaviorSubject<Boolean> overrideConfiguration = BehaviorSubject.create();
        private ConfigurationFetcher configurationFetcher;

        ConfigurationLoader(ConfigurationFetcher fetcher) {
            configurationFetcher = fetcher;
            overrideConfiguration.onNext(false);
        }

        Observable<String> configuration = overrideConfiguration
                .switchMap(new Function<Boolean, ObservableSource<? extends String>>() {
                    @Override
                    public ObservableSource<? extends String> apply(Boolean override) throws Throwable {
                        if (override) {
                            return Observable.just("Overrided");
                        } else {
                            return configurationFetcher.configurationData;
                        }
                    }
                });
    }

    static class ConfigurationFetcher {
        Observable<String> configurationData = Observable.fromPublisher(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> subscriber) {
                RemoteCallback listener = new RemoteCallback() {
                    @Override
                    public void onSuccess(String json) {
                        subscriber.onNext(json);
                        subscriber.onComplete();
                    }
                };
                new RequestCallable().request(listener);
            }
        });
    }

    interface RemoteCallback {
        void onSuccess(String json);
    }

    static class RequestCallable {
        void request(RemoteCallback callback) {
            callback.onSuccess("Fetched");
        }
    }
}
