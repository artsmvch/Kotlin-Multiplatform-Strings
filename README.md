# Kotlin Multiplatform Strings

This is a kotlin multiplatform plugin that generates localized strings from xml resources.

## Getting started

### Step 1: apply the strings plugin

In the shared module, apply the strings plugin:
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    // The strings plugin
    id("com.artsmvch.strings")
}
```

And configure the strings extension:
```kotlin
strings {
    // Specify all languages your app supports
    supportedLanguages = setOf("en", "es", "pl")
    // Package in which the code will be generated
    packageName = "com.strings.sample"
    // Missing translation strategy
    missingTranslationStrategy = MissingTranslationStrategy.FAIL_BUILD
}
```

### Step 2: define string resources

Resources are defined in xml files the same way as in the Android project:
```
<resources>
    <!--String-->
    <string name="hello_world">Hello World!</string>
    
    <!--Plurals-->
    <plurals name="clicked_n_times">
        <item quantity="one">Clicked %1$d time</item>
        <item quantity="other">Clicked %1$d times</item>
    </plurals>
    
    <!--String array-->
    <string-array name="colors_array">
        <item>Blue</item>
        <item>Red</item>
        <item>Green</item>
    </string-array>
    ...
</resources>
```

The following resource types are supported:
* strings
* plurals
* string arrays

The resource files are then placed in the appropriate resource folders with the desired language tags:
```
.
└── src/
    ├── androidMain
    ├── iosMain
    └── commonMain/
        ├── kotlin
        └── composeResources/
            ├── values-en/
            │   └── string.xml
            ├── values-es/
            │   └── string.xml
            └── values-pl/
                └── string.xml
```

### Step 3: generate strings code

Then you need to run the `generateStrings` task on the module with the strings:
`./gradlew :shared:generateStrings`

This will generate Strings interface for accessing resources:
```kotlin
package com.strings.sample

import kotlin.Int
import kotlin.String
import kotlin.collections.List

public interface Strings {
  public val hello_world: String

  public fun clicked_n_times(count: Int, arg1: Int = count): String

  public fun colors_array(): List<String>
}

```

And Strings objects for each language:
```kotlin
package com.strings.sample

import kotlin.Int
import kotlin.String
import kotlin.collections.List

internal object enStrings : Strings {
    override val hello_world: String = "Hello World!"

    override fun clicked_n_times(count: Int, arg1: Int): String {
        val value = when {
            count == 1 -> "Clicked %1${'$'}d time"
            else -> "Clicked %1${'$'}d times"
        }
        return value.format(arg1)
    }

    override fun colors_array(): List<String> {
        val list = ArrayList<String>(3)
        list.add("Blue")
        list.add("Red")
        list.add("Green")
        return list
    }
}
```

### Step 4: use the generated strings code

You can now use the generated code like this:
```kotlin
private const val LANG_EN = "en"
private const val LANG_ES = "es"
private const val LANG_PL = "pl"

expect fun getSystemLangCodeStateFlow(): StateFlow<String?>

@Composable
internal fun strings(): Strings {
    val lang = getSystemLangCodeStateFlow().collectAsState().value
    return when(lang) {
        LANG_EN -> enStrings
        LANG_ES -> esStrings
        LANG_PL -> plStrings
        else -> enStrings
    }
}

@Composable
fun SampleScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = strings().hello_world, style = MaterialTheme.typography.h3)
        Spacer(Modifier.height(16.dp))
        // Accessing the Strings interface to get required resource
        strings().colors_array().forEach {
            Text(text = it, style = MaterialTheme.typography.body1)
        }
    }
}
```

The step 3 can actually be skipped as it is configured and run by the plugin as part of the compilation process. 






