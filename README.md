更加精简的安卓PDF阅读器，符合实际使用需求。相比上游项目删除了谷歌引擎，实现双指放大缩小能力。

Simpler Android pdf viewer, fit with actual demand. Delete google engine and append the ability of zoom in or zoom out with two fingers rather than upstream.

用法/Usage：

1. Add this to build.gradle(app)
```plain
implementation 'com.github.g39088902:Pdf-Viewer:8bf1bbd59d'
```
2. Add pdfView to your layout.cml and add this to your Activity
```plain
pdfView.initWithFile(file)
```
3. for more detail, read the code.

 

