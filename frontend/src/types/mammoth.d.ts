declare module 'mammoth' {
  interface ConversionResult {
    value: string
    messages: Array<{ type: string; message: string }>
  }

  interface ConvertToHtmlOptions {
    arrayBuffer?: ArrayBuffer
  }

  export function convertToHtml(options: ConvertToHtmlOptions): Promise<ConversionResult>
}
