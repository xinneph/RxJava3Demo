import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject

private val sampleJson = "\"isMessagingEnabled\": true}"
private val overrideConfiguration = BehaviorSubject.create<Boolean>().apply { onNext(true) }

fun main() {
    val configurationFetcher = ConfigurationFetcher()
    val configurationLoader = ConfigurationLoader(configurationFetcher)
    configurationLoader.configuration.subscribe()
}

class ConfigurationLoader(private val configurationFetcher: ConfigurationFetcher) {
    val configuration: Observable<String> = overrideConfiguration
        .switchMap { override ->
            if (override) {
                Observable.just(sampleJson)
            } else {
                configurationFetcher.configurationData
            }
        }
}

class ConfigurationFetcher {
    val configurationData: Observable<String> = Observable.fromPublisher { subscriber ->
        val listener = object : RemoteCallback {
            override fun onSuccess(json: String) {
                subscriber.onNext(json)
                subscriber.onComplete()
            }
        }
        RequestCallable().request(listener)
    }
}

interface RemoteCallback {
    fun onSuccess(json: String)
}

class RequestCallable {
    fun request(callback: RemoteCallback) {
        callback.onSuccess(sampleJson)
    }
}
