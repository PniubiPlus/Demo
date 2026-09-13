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

## 接口响应与 JSON 序列化

上传解析接口 `POST /api/parse` 使用 Jackson（`jackson-databind`）序列化成功和失败响应，不再手动拼接 JSON 字符串。这样 PDFBox 提取文本中的换行符、制表符及其他 JSON 控制字符会被正确转义，前端可以稳定地通过 `response.json()` 读取结果。

成功响应字段为 `fileName`、`pageCount`、`characterCount` 和 `text`；失败响应统一返回 JSON 对象，其中 `message` 字段描述失败原因。

## 代码结构

- `PdfParser`：PDF 解析抽象接口。
- `PdfBoxParser`：基于 Apache PDFBox 的原生文字解析实现。
- `PdfDocument`：解析结果模型，为后续 RAG 流程提供统一输入。
- `RagPdfApplication`：命令行入口，并预留文本切分、向量化、知识库入库位置。
