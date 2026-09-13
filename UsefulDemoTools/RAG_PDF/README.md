# RAG PDF 解析工具

当前版本只负责读取原生文字 PDF，并提取文本内容；不处理扫描件 OCR、表格，也不执行知识库入库。

## 环境

- Java 17+
- Maven 3.9+

## 使用

项目使用内置 HTTP 服务提供上传页面。在本目录执行：

```bash
mvn clean package
mvn exec:java
```

然后打开 <http://localhost:8080>，选择或拖拽本地 PDF 并点击“开始解析”。也可以指定端口：

```bash
mvn exec:java -Dexec.args="9090"
```

解析结果包含文件名、页数、字符数和提取出的全文。命令行直接解析文件的入口后续可继续补充。

## 代码结构

- `PdfParser`：PDF 解析抽象接口。
- `PdfBoxParser`：基于 Apache PDFBox 的原生文字解析实现。
- `PdfDocument`：解析结果模型，为后续 RAG 流程提供统一输入。
- `RagPdfApplication`：命令行入口，并预留文本切分、向量化、知识库入库位置。
