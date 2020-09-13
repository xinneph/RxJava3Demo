import com.google.gson.Gson
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject


fun main() {
    val localConfigurationFetcher = LocalConfigurationFetcher()
    val remoteConfigurationFetcher = RemoteConfigurationFetcher()
    val configurationFetcher: ConfigurationFetcher = DefaultConfigurationFetcher(localConfigurationFetcher, remoteConfigurationFetcher)
    val configurationLoader = ConfigurationLoader(remoteConfigurationFetcher)
    configurationLoader.configuration.subscribe()
}

class ConfigurationLoader(private val configurationFetcher: ConfigurationFetcher) {
    private val overrideConfiguration = BehaviorSubject.create<Boolean>().apply { onNext(true) }
    val configuration: Observable<ConfigurationData> = overrideConfiguration
        .distinctUntilChanged()
        .switchMap { configurationFetcher.configurationData }

//    val configuration: Observable<ConfigurationData> = configurationFetcher.configurationData
}

interface ConfigurationFetcher {
    val configurationData: Observable<ConfigurationData>
}

data class ConfigurationData(val isMessagingEnabled: Boolean)

class RemoteConfigurationFetcher : ConfigurationFetcher {
    override val configurationData: Observable<ConfigurationData> = Observable.fromPublisher<String> { subscriber ->
        val listener = object : RemoteCallback {
            override fun onSuccess(json: String) {
                subscriber.onNext(json)
                subscriber.onComplete()
            }
        }
        RequestCallable().request(listener)
    }.flatMapSingle { ConfigurationParser().parse(it) }
}

interface RemoteCallback {
    fun onSuccess(json: String)
}

class RequestCallable {
    fun request(callback: RemoteCallback) {
        callback.onSuccess("{\"isMessagingEnabled\": true}")
    }
}

class ConfigurationParser {
    fun parse(json: String): Single<ConfigurationData> = Single.fromCallable {
        Gson().fromJson(json, ConfigurationData::class.java)
    }
}

class LocalConfigurationFetcher : ConfigurationFetcher {
    override val configurationData: Observable<ConfigurationData> = Observable.fromCallable {
//        ConfigurationData(true)
        throw Throwable("Local configuration is empty")
    }
}

class DefaultConfigurationFetcher(local: LocalConfigurationFetcher, remote: RemoteConfigurationFetcher) : ConfigurationFetcher {
    override val configurationData: Observable<ConfigurationData> = local.configurationData
        .onErrorResumeWith(remote.configurationData)
}
