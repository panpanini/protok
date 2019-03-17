# protok
Kotlin code generator plugin for protoc.

This project is currently still in beta - please use at your own risk.

## Usage

### Compiler
You can use the compiler by downloading the latest version from the releases tab. 

Once you have downloaded the compiler plugin, you should add the plugin to your PATH so that protoc can access it. For example, you could add the following to your `.bash_profile`:
``` sh
export PATH=${PATH}:<path-to-install-location>/protoc-gen-kotlin/bin
```

Once that is done, you only need to add the `--kotlin_out` option to your `protoc` command, and the generator will create kotlin models in the specified location.
``` sh
protoc --kotlin_out=path/to/kotlin/out input.proto
```

### Runtime
The runtime is required for any implementations using the generated Kotlin models, as it contains the definitions for the required parent classes. please see the `runtime` module for more information. This will be uploaded to maven central in the future.
``` groovy
implementation ('jp.co.panpanini:protok-runtime:0.0.9')
```

### Retrofit Converter
The `retrofit-converter` module is a Retrofit Converter factory, which can be used alongside Retrofit to marshal/unmarshal any requests/responses made through Retrofit. After adding the dependency in gradle:
``` groovy
implementation ('jp.co.panpanini:protok-retrofit-converter:0.0.30')
```
It is as simple as adding the following line to your Retrofit builder:
``` kotlin
Retrofit.Builder()
    .addConverterFactory(ProtokConverterFactory.create())
    ...
    .build()
```

## TODO
- custom services for RPC
- refactor code generated using only strings (use KotlinPoet correctly)
- ensure both proto2 and proto3 support


## Acknowledgments 
This project is heavily influenced by [pbandk](https://github.com/cretz/pb-and-k/). For a closer-to-finished solution, please take a look!
