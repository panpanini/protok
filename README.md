# protok
Kotlin code generator plugin for protoc.

This project is currently still in beta - please use at your own risk.

## Usage
You can use this as a plugin by building from source. To build the library, you can use the following gradle command:
``` bash
./gradlew library:installDist
```
This will build the library, and install it to the `build` folder. You can then use this built library with `protoc`:
``` bash
protoc --plugin=protoc-gen-custom=protok/bin/protok
```

Once the models have been generated, please use the `runtime` library in order to resolve the base `Message` classes.
``` groovy
implementation ('jp.co.panpanini:protok-runtime:0.0.9')
```

There is also a retrofit converter factory available as well, available here:
``` groovy
implementation ('jp.co.panpanini:protok-retrofit-converter:0.0.30')
```

## TODO
- support `oneof` parameters
- clean up code
- refactor code generated using only strings (use KotlinPoet correctly)
- tests 😇
