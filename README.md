## Glyph Matrix SDK (Nothing) — Geliştirici Rehberi

Bu belge, `kit/glyph-matrix-sdk-1.0.aar` ile gelen Nothing Glyph Matrix SDK’sını kullanarak Nothing telefonların arka yüzündeki LED matrisinde nasıl içerik gösterebileceğinizi ayrıntılı biçimde açıklar. Projedeki örnek akışlar (`toy` servis ve "App Matrix" render) üzerinden, pratik kullanım senaryolarına ve en iyi uygulamalara değinilir.

### İçindekiler
- **Genel Bakış**
- **Destek ve Gereksinimler**
- **SDK’daki Temel Sınıflar ve API’ler**
- **Hızlı Başlangıç**
- **Kurulum ve Entegrasyon**
- **Cihaz Kaydı ve Servis Bağlantısı**
- **Kare Yapısı: `GlyphMatrixFrame` ve Katmanlar**
- **Nesne Yapısı: `GlyphMatrixObject` ve Özellikler**
- **Metin Kaydırma (Marquee) ve Stratejiler**
- **App Matrix vs. Matrix**
- **Kapanış/Timeout ve Kaynak Yönetimi**
- **Hata Yönetimi ve Dayanıklılık**
- **Performans İpuçları**
- **Örnekler**
- **Sık Sorulan Sorular**
- **Bilinen Kısıtlar**

---

### Genel Bakış
Nothing’ın Glyph Matrix özelliği, telefonun arka yüzündeki LED matrisine özel frame’ler göndererek metin, ikon veya özel animasyonlar göstermeye olanak tanır. Bu SDK:

- LED matrisine ait frame verisini oluşturmanızı (`GlyphMatrixFrame`) ve göndermenizi (`GlyphMatrixManager`) sağlar.
- Metin, resim ve basit animasyonlar için bir nesne modeli (`GlyphMatrixObject`) sunar.
- İsterseniz kayan yazı (marquee) için yüksek seviyeli bir sınıf (`GlyphMatrixFrameWithMarquee`) ya da manuel animasyon için düşük seviyeli set’ler sağlayabilir.

### Destek ve Gereksinimler
- **Cihaz**: Nothing telefonlar (SDK’daki `Glyph.DEVICE_20111`, `22111`, `23111`, `24111`, `23112`, `23113` gibi cihaz tanımları mevcut). Hangi cihazda çalışacağınızı uygun sabit ile kaydetmeniz gerekir.
- **OS Servisi**: SDK, sistem servisleriyle (AIDL: `IGlyphService`) haberleşir. Bu nedenle Nothing cihaz ve destekleyen Nothing OS sürümü gereklidir.
- **Android Proje Tipi**: Android app/modül (Kotlin veya Java) ile kullanılabilir.

### SDK’daki Temel Sınıflar ve API’ler

- **`com.nothing.ketchum.GlyphMatrixManager`**
  - `getInstance(Context)`: Tekil yönetici örneği.
  - `init(Callback) / unInit()`: Sistem servisine bağlan/ayrıl.
  - `register(String deviceCode)`: Hedef cihazı bildir (örn. `Glyph.DEVICE_23111`).
  - `setMatrixFrame(GlyphMatrixFrame | int[])`: Frame’i LED matrisine gönder.
  - `setAppMatrixFrame(GlyphMatrixFrame | int[])`: App Matrix kanalına frame gönder.
  - `closeAppMatrix()`: App Matrix görüntüsünü kapat.
  - `setGlyphMatrixTimeout(boolean)`: Otomatik zaman aşımı davranışı.
  - `turnOff()`: Matrisi kapat.

- **`com.nothing.ketchum.GlyphMatrixFrame`**
  - Katman mantığına sahiptir: top/mid/low nesne veya doğrudan `int[]` dizileri.
  - `render()`: Frame’i cihazın beklediği piksel dizisine dönüştürür.
  - Genellikle `GlyphMatrixFrame.Builder()` ile oluşturulur ve `build(context)` ile tamamlanır.

- **`com.nothing.ketchum.GlyphMatrixFrameWithMarquee`**
  - `GlyphMatrixFrame` türevi; metin kaydırma için süre (duration) ve adım (step) kontrolü sunar.
  - `updateDuration(int)`, `updateStep(int)`, `stopMarquee()` gibi ek denetimler vardır.

- **`com.nothing.ketchum.GlyphMatrixObject`**
  - Metin veya bitmap tabanlı içeriği, konum, ölçek, parlaklık gibi parametrelerle temsil eder.
  - Öne çıkan getter’lar: `getText()`, `getImageSource()`, `getPositionX/Y()`, `getScale()`, `getBrightness()`, `getMarqueeType()` vb.
  - Marquee tip sabitleri: `TYPE_MARQUEE_NONE`, `TYPE_MARQUEE_FORCE`, `TYPE_MARQUEE_AUTO`.
  - Genellikle `GlyphMatrixObject.Builder()` ile üretilir.

- **`com.nothing.ketchum.Glyph`**
  - Cihaz tanımları: `DEVICE_20111`, `DEVICE_22111`, `DEVICE_23111`, `DEVICE_24111`, `DEVICE_23112`, `DEVICE_23113` vb.

### Hızlı Başlangıç
1) **Yöneticiyi al ve başlat**: `GlyphMatrixManager.getInstance(context).init(callback)`
2) **Cihaz kaydı**: `manager.register(Glyph.DEVICE_XXXX)`
3) **Frame oluştur**: `GlyphMatrixFrame.Builder().addTop(obj).build(context)`
4) **Gönder**: `manager.setMatrixFrame(frame)` veya `manager.setAppMatrixFrame(frame)`

### Kurulum ve Entegrasyon
- `kit/glyph-matrix-sdk-1.0.aar` dosyasını modülünüze ekleyin. Örnek bir yaklaşım:
  - `app/build.gradle.kts` içinde `implementation(files("../kit/glyph-matrix-sdk-1.0.aar"))` benzeri bir tanım kullanın (organizasyon projenize göre değişebilir).
  - Gerekirse `flatDir` repo tanımı ekleyin veya AAR’ı `libs/` altına koyun.
- AndroidManifest ayarları:
  - Uygulamanız bir servis üzerinden çalışacaksa servis manifest girişini yapın.
  - Özel izin gerekmiyorsa dahi, servis bağlanması için `BIND`/`FOREGROUND` gibi tipik Android kısıtlarını gözden geçirin.
- Minimum SDK/Target SDK ve Gradle ayarlarınızı Nothing cihaz hedeflerinizle uyumlu tutun.

### Cihaz Kaydı ve Servis Bağlantısı
Bağlantı akışı tipik olarak şöyledir:

```kotlin
val manager = GlyphMatrixManager.getInstance(applicationContext)
manager.init(object : GlyphMatrixManager.Callback {
    override fun onServiceConnected(name: ComponentName) {
        manager.register(Glyph.DEVICE_23111)
        // Artık frame gönderebilirsiniz
    }
    override fun onServiceDisconnected(name: ComponentName) { }
})
```

Notlar:
- Cihaz kodunu doğru seçmek önemlidir. Projenizde hedeflediğiniz Nothing modele uygun `Glyph.DEVICE_…` sabitini kullanın.
- `unInit()` ve `turnOff()` çağrılarını yaşam döngüsünde doğru yerde yapmak, servis ve kaynak sızıntılarını engeller.

### Kare Yapısı: `GlyphMatrixFrame` ve Katmanlar
`GlyphMatrixFrame`, tek bir frame’i temsil eder ve genelde üç seviyeli içerik katmanına sahiptir:

- top
- mid
- low

Her seviyeye ya bir `GlyphMatrixObject` yerleştirilebilir ya da doğrudan piksel dizisi (`int[]`) atanabilir. Bu esneklik, basit metin veya bitmapi kolayca üst üste bindirmenize olanak tanır. Tipik kullanım:

```kotlin
val obj = GlyphMatrixObject.Builder()
    .setText("Hello")
    .setPosition(25, 10)
    .setScale(100)
    .setBrightness(255)
    .build()

val frame = GlyphMatrixFrame.Builder()
    .addTop(obj)
    .build(context)

manager.setMatrixFrame(frame)
```

Gelişmiş durumda `GlyphMatrixFrameWithMarquee` sınıfı ile `top/mid/low` için hem nesne hem de dizi tabanlı set’ler yapılabilir; ayrıca marquee süre/adım ayarı desteklenir.

### Nesne Yapısı: `GlyphMatrixObject` ve Özellikler
`GlyphMatrixObject`, render edilecek içerik için şu temel özellikleri sunar:

- **Konum**: `setPosition(x, y)` ile başlangıç konumu. Koordinat sistemi cihaz modeline göre değişebilir; genelde soldan-sağa X artar, yukarıdan-aşağı Y artar.
- **Ölçek**: `setScale(%)` piksel yazımını büyütüp/küçültür. 100 tipik boyuttur.
- **Parlaklık**: `setBrightness(0..255)` içerik parlaklığı.
- **Metin**: `setText("...")` ile metin yazdırma. Yazı tipi/stili `setTextStyle()` gibi alanlarla uyumlu cihaz/sürümde çeşitlendirilebilir.
- **Marquee**: `setMarqueeType(TYPE_MARQUEE_NONE|AUTO|FORCE)` ve `setMarqueeRange(Range)` gibi ek seçenekler (SDK sürüm desteğine bağlı olarak değişebilir).
- **Bitmap**: `setImageSource(bitmap)` benzeri API’ler ile görsel basabilirsiniz (kullanımınızda `getImageSource()` gözükmektedir, builder tarafı sürüme göre değişiklik gösterebilir).

Boyut ve hücre sayıları cihaz bazında farklı olduğundan, metin genişliğini kestirmek için pratik bir yaklaşım kullanabilirsiniz. Örneğin projede kullanılan kaba bir tahmin:

```kotlin
private fun estimateTextWidthCells(text: String): Int =
    (text.length * 6).coerceAtLeast(8).coerceAtMost(120)
```

Bu, animasyon zamanlamalarını ayarlarken işe yarar bir yaklaşık değerdir.

### Metin Kaydırma (Marquee) ve Stratejiler
Marquee için iki yaklaşım vardır:

1) **Manuel Kaydırma**: Bir `Handler`/zamanlayıcı ile X pozisyonunu her adımda değiştirip yeni frame gönderirsiniz.
   - Esnek, tamamen kontrol sizde.
   - Örnek akış: her 25–70 ms arası bir adım, X’i 1 piksel azalt, bitince başa al.

2) **`GlyphMatrixFrameWithMarquee`**: SDK’nın marquee sınıfını kullanırsınız.
   - `updateDuration(int)` ve `updateStep(int)` ile hız ve adımı kontrol edin.
   - `stopMarquee()` ile kaydırmayı durdurun.

Manuel örnek (özet):

```kotlin
val handler = Handler(Looper.getMainLooper())
var scrollX = 25
val text = "Merhaba"

fun tick() {
    val obj = GlyphMatrixObject.Builder()
        .setText(text)
        .setPosition(scrollX, 10)
        .setScale(100)
        .setBrightness(255)
        .build()

    val frame = GlyphMatrixFrame.Builder()
        .addTop(obj)
        .build(context)

    manager.setMatrixFrame(frame)

    scrollX -= 1
    val delay = 40L // metin uzunluğuna göre 25..70 ms arası ayarlayabilirsiniz
    handler.postDelayed({ tick() }, delay)
}
```

### App Matrix vs. Matrix
SDK iki farklı gönderim yolu sunar:

- **`setMatrixFrame(...)`**: Genel matris kanalına frame gönderir. Genellikle "Toy" veya sistem genel kullanımına benzer bir yaklaşım.
- **`setAppMatrixFrame(...)`**: Uygulama bağlamlı (App Matrix) bir kanaldır. Uygulama-odaklı kısa gösterimler için uygundur; `closeAppMatrix()` ile kapanış kontrolü sizde olur.

Uygulamanızda yalnızca kısa süreli bir metin/pattern gösterecekseniz `setAppMatrixFrame` + `closeAppMatrix()` iyi bir ikilidir.

### Kapanış/Timeout ve Kaynak Yönetimi
- **Zaman Aşımı**: `setGlyphMatrixTimeout(boolean)` ile otomatik kapanışı açıp kapatabilirsiniz.
- **Kapanış**: App Matrix için `closeAppMatrix()`; genel kanal için `turnOff()`.
- **Yaşam Döngüsü**: Servis/Activity biterken `handler.removeCallbacksAndMessages(null)`, `unInit()`, `turnOff()` gibi çağrıları ihmal etmeyin.

### Hata Yönetimi ve Dayanıklılık
- `setMatrixFrame(...)` ve `setAppMatrixFrame(...)` çağrıları `GlyphException` fırlatabilir. Yakalayın ve geriye dönüş (fallback) davranışı planlayın.
- Servis bağlantısı koparsa (`onServiceDisconnected`), yeniden başlatma veya sıraya alma (pending) stratejisi uygulayın.

### Performans İpuçları
- **Adım Süresi**: 25–70 ms arası gecikmeler, akıcı ve pil dostu sonuçlar sağlar. Metin uzunluğunuza göre dinamik ayarlayın.
- **Aşırı Frame**: Çok sık frame göndermek pil tüketimini artırır ve UI thread’i meşgul eder. Gerekliyse tek bir `Handler` kullanın.
- **Katmanlar**: Gereksiz katman kullanmayın; tek nesne yeterliyse `top` katmanı kâfi.
- **Ölçek/Parlaklık**: Aşırı yüksek parlaklık pil tüketimini artırır; ölçek ile metin okunurluğunu dengeleyin.

### Örnekler
- **Toy Servisi ile sürekli kaydırma**: `app/src/main/java/com/efedonmez/nothingmatrixmusicdisc/toy/MatrixDemoToyService.kt` içerisinde servis bağlanır, metin alınır ve sürekli scroll yapılır.
- **App Matrix ile tek geçiş ve otomatik kapanış**: `app/src/main/java/com/efedonmez/nothingmatrixmusicdisc/appmatrix/AppMatrixRenderer.kt` metni bir kez kaydırıp `closeAppMatrix()` ile kapatır.

İkisinin temel farkı, Toy servisinde sürekli animasyon akışı varken App Matrix senaryosunda kısa süreli gösterim ve kapanış yönetimi öne çıkar.

### Resim Ekleme (Bitmap → int[])
Metin dışında bitmap tabanlı içerik göstermek için iki pratik yol vardır:

- `GlyphMatrixObject` builder’ında varsa `setImageSource(bitmap)` kullanmak.
- Veya `GlyphMatrixUtils.convertToGlyphMatrix(bitmap, ...)` ile bir `int[]` piksel dizisi üretip doğrudan göndermek.

Bitmap’i diziye çevirip göndermek:

```kotlin
// 1) Kaynak: Drawable → Bitmap
val drawable = AppCompatResources.getDrawable(context, R.drawable.my_logo)!!
val bitmap = GlyphMatrixUtils.drawableToBitmap(drawable)

// 2) İsteğe bağlı: Gri tonlamaya çevirme / ön işleme
// val gray = GlyphMatrixUtils.toGrayscaleBitmap(bitmap, 128)
// val morph = GlyphMatrixUtils.erosion(gray)

// 3) Bitmap → Glyph piksel dizisi
// Parametreler (özet): genişlik, yükseklik, parlaklık/ölçek/konum vb. için sayısal alanlar ve 2 adet boolean bayrak
// Bu parametreler sürüme göre farklılık gösterebilir; güvenli varsayılan değerlerle başlayıp görsel sonucu test ederek ayarlayın.
val pixels: IntArray = GlyphMatrixUtils.convertToGlyphMatrix(
    bitmap,
    /*width*/ 0, /*height*/ 0,
    /*scaleOrBrightness*/ 100,
    /*offsetOrThreshold*/ 0,
    /*extraLevel*/ 255,
    /*flipX*/ false,
    /*flipY*/ false
)

// 4) Gönderim (genel kanal)
GlyphMatrixManager.getInstance(context).setMatrixFrame(pixels)

// Alternatif: App Matrix kanalı
// GlyphMatrixManager.getInstance(context).setAppMatrixFrame(pixels)
```

Notlar:
- `GlyphMatrixUtils.MATRIX_SIZE` sabiti toplam hücre sayısını ifade eder; cihaz modeline göre konum dizilimi farklı olabilir.
- Dönüşüm parametrelerini (parlaklık eşiği, ölçek, yatay/dikey çevirme) görsel sonucu test ederek ayarlayın.
- Cihaz grid boyutu ve yönü için üretici dokümantasyonuna bakın.

### Tekil Piksel Erişimi (int[] ile)
Piksellere tek tek erişmek için doğrudan `int[]` kullanabilirsiniz. Dizinin boyutu `GlyphMatrixUtils.MATRIX_SIZE`’dır. Cihaz grid’inin sütun/satır dizilimi modele göre değişir; tipik olarak soldan-sağa satır-majör bir dizilim kullanılır, ancak cihazınıza göre yatay/dikey ekseni terslemeniz gerekebilir.

Örnek yaklaşım (grid boyutunu cihaz dokümanından alın):

```kotlin
// Cihazınıza uygun grid boyutlarını belirleyin (örneğin W x H)
const val GRID_WIDTH = 40  // örnek
const val GRID_HEIGHT = 20 // örnek

val pixels = IntArray(GlyphMatrixUtils.MATRIX_SIZE) { 0 }

fun indexOf(x: Int, y: Int): Int {
    // Satır-majör: her satırda W piksel
    return y * GRID_WIDTH + x
}

fun setPixel(x: Int, y: Int, value: Int) {
    if (x !in 0 until GRID_WIDTH || y !in 0 until GRID_HEIGHT) return
    pixels[indexOf(x, y)] = value // value: genelde 0..255 parlaklık veya cihazın beklediği yoğunluk
}

// Basit bir diyagonal çizgi
for (i in 0 until min(GRID_WIDTH, GRID_HEIGHT)) {
    setPixel(i, i, 255)
}

// Gönder
GlyphMatrixManager.getInstance(context).setMatrixFrame(pixels)
```

Yatay/dikey eksen tersleme ihtiyacı varsa `indexOf` fonksiyonunu aşağıdaki gibi ayarlayın:

```kotlin
fun indexOfFlipped(x: Int, y: Int): Int {
    val fx = GRID_WIDTH - 1 - x // yatay flip
    val fy = y                  // gerekirse dikey flip: GRID_HEIGHT - 1 - y
    return fy * GRID_WIDTH + fx
}
```

İpucu: Bazı cihaz/sürümlerde `GlyphMatrixObject` içinde `setReverse(...)` veya `setOrientation(...)` benzeri builder ayarları bulunabilir; bu durumda pikselleri elle çevirmek yerine nesne yönünü ayarlamayı tercih edin.

### Sık Sorulan Sorular
- **Matris boyutu kaç piksel?**
  - Cihaza göre değişir. Projeye ekli görseller (`kit/image/*Glyph Matrix*svg`) matris yerleşimi hakkında bilgi verir. Programatik olarak kesin boyut değeri için yayınlanmış public bir sabit olmayabilir; pratikte deneme/yanılma veya örneklerden yararlanabilirsiniz.
- **Metin fontu/stili değişir mi?**
  - SDK’da `getTextStyle()` görülüyor; ancak stil setleme olanakları cihaz/OS sürümüne göre değişebilir. Basit metin kullanımını hedefleyin.
- **Bitmap gösterebilir miyim?**
  - Evet; `GlyphMatrixObject` bitmap içeriği destekler. Boyut/ölçek cihaz matrisine göre uyarlanmalıdır.

### Referanslar
- [Nothing Community: Glyph Matrix Developer Kit](https://nothing.community/d/35633-glyph-matrix-developer-kit)

### Bilinen Kısıtlar
- SDK, Nothing cihaz ve servislerine bağımlıdır; diğer cihazlarda çalışmaz.
- Cihaz modeline göre matris yerleşimi ve çözünürlük farklılık gösterebilir. Kodunuzu model bazlı esnek tasarlayın.
- Aşırı parlaklık ve sık frame gönderimi pil tüketimini artırır; sorumlu kullanım önerilir.

---

## Hızlı Kod Parçaları

### Başlatma ve Kayıt
```kotlin
val manager = GlyphMatrixManager.getInstance(applicationContext)
manager.init(object : GlyphMatrixManager.Callback {
    override fun onServiceConnected(name: ComponentName) {
        manager.register(Glyph.DEVICE_23111)
    }
    override fun onServiceDisconnected(name: ComponentName) {}
})
```

### Basit Metin Gösterimi
```kotlin
val obj = GlyphMatrixObject.Builder()
    .setText("Nothing")
    .setPosition(25, 10)
    .setScale(100)
    .setBrightness(255)
    .build()

val frame = GlyphMatrixFrame.Builder()
    .addTop(obj)
    .build(context)

manager.setMatrixFrame(frame)
```

### App Matrix ile Göster ve Kapat
```kotlin
manager.setAppMatrixFrame(frame)
// ... bir süre sonra
manager.closeAppMatrix()
```

---

## Ek Kaynaklar
- Proje içi görseller: `kit/image/` klasörü (matris yerleşimi ve ikon ölçüleri için referans)
- Örnek kodlar: `toy/MatrixDemoToyService.kt`, `appmatrix/AppMatrixRenderer.kt`

Sorularınız veya genişletmek istediğiniz senaryolar için issue/PR açabilirsiniz.


