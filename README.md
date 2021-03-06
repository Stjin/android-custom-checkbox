# Android Custom CheckBox

Android custom checkbox based on [SmoothCheckBox](https://github.com/andyxialm/SmoothCheckBox)

Add it in your root build.gradle at the end of repositories:

```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

*Step 2*. Add the dependency

```
dependencies {
    implementation 'com.github.Stjin:android-custom-checkbox:Tag'
}
```

![](https://github.com/Stjin/android-custom-checkbox/blob/master/assets/smoothcb.gif?raw=true)

## Attrs

|Attr|Type|Description|
|---|:---|:---:|
|duration|integer|Animation Duration|
|stroke_width|dimension|The border width when unchecked|
|color_tick|color|Tick color (visible only when checked)|
|color_checked|color|Fill color when selected|
|color_unchecked|color|Fill color when unchecked|
|color_unchecked_stroke|color|Border color when unchecked|

## Sample Usage

```java
setChecked(boolean checked); //by default, it's wthout animation
        setChecked(boolean checked,boolean animate);  //pass true to animate
```

```java
protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

final CustomCheckBox scb=(CustomCheckBox)findViewById(R.id.scb);
        scb.setOnCheckedChangeListener(new CustomCheckBox.OnCheckedChangeListener(){
@Override
public void onCheckedChanged(CustomCheckBox checkBox,boolean isChecked){
        Log.d("CustomCheckBox",String.valueOf(isChecked));
        }
        });
        }
```