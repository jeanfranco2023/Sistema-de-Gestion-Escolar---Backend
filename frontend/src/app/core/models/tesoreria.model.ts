export type EstadoObligacion = 'PENDIENTE' | 'PAGADO_TOTAL' | 'PAGADO_PARCIAL' | 'VENCIDO' | 'CANCELADO_POR_TRASLADO';
export type TipoConcepto = 'MATRICULA' | 'PENSION' | 'CERTIFICADO' | 'OTRO';
export type MetodoPago = 'EFECTIVO' | 'TRANSFERENCIA' | 'TARJETA_CREDITO' | 'TARJETA_DEBITO' | 'YAPE' | 'PLIN';
export type EstadoPago = 'APROBADO' | 'RECHAZADO' | 'REVERTIDO';
export type PasarelaProveedor = 'CULQI' | 'NIUBIZ' | 'MERCADO_PAGO' | 'CAJA_EFECTIVO' | 'TRANSFERENCIA';
export type TipoComprobante = 'BOLETA' | 'RECIBO_INTERNO';
export type EstadoComprobante = 'EMITIDO' | 'ANULADO';
export interface ConceptoCobroResponseDto { id: number; codigo: string; nombre: string; tipoConcepto: TipoConcepto; montoSugerido: number; }
export interface GenerarObligacionesAnualesRequestDto { matriculaId: number; conceptoMatriculaId: number; conceptoPensionId: number; primerVencimiento: string; }

export interface ObligacionResponseDto {
  id: number;
  matriculaId: number;
  conceptoId?: number;
  tipoConcepto: TipoConcepto;
  numeroCuota?: number;
  descripcion: string;
  fechaVencimiento: string;
  montoBase: number;
  montoMora: number;
  montoDescuento: number;
  totalPagado: number;
  saldoPendiente: number;
  estado: EstadoObligacion;
}

export interface TransaccionResponseDto {
  id: number;
  uuid?: string;
  obligacionPagoId: number;
  pasarelaProveedor: PasarelaProveedor;
  pasarelaTransaccionId?: string;
  metodoPago: MetodoPago;
  montoPagado: number;
  fechaPago: string;
  estadoPago: EstadoPago;
  payloadWebhook?: Record<string, any>;
}

export interface PreferenciaMercadoPagoResponseDto {
  preferenceId: string;
  initPoint: string;
  sandboxInitPoint: string;
  publicKey: string;
}

export interface ComprobanteResponseDto {
  id: number;
  uuid: string;
  pagoTransaccionId: number;
  tipoComprobante: TipoComprobante;
  serie: string;
  correlativo: number;
  fechaEmision: string;
  montoTotal: number;
  estadoComprobante: EstadoComprobante;
  fechaAnulacion?: string;
  motivoAnulacion?: string;
  urlPdfComprobante?: string;
}

export interface RegistrarPagoCajaRequestDto {
  obligacionPagoId: number;
  pasarelaTransaccionId: string;
  metodoPago: MetodoPago;
  montoPagado: number;
}

export interface CrearPreferenciaMercadoPagoDto {
  obligacionPagoId: number;
  backUrlSuccess?: string;
  backUrlFailure?: string;
}
