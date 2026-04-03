// User types
export interface User {
  id: number
  name: string
  email: string
  role: 'USER' | 'ADMIN' | 'SUPERADMIN'
  createdAt?: string
  orderCount?: number
}

// Auth types
export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
}

export interface AuthResponse {
  token: string
  type: string
  userId: number
  name: string
  email: string
  role: string
}

// Order types
export type OrderStatus = 
  | 'PENDIENTE_PAGO'
  | 'PAGADO'
  | 'PEDIDO_REALIZADO'
  | 'LLEGO_BODEGA_USA'
  | 'EN_TRANSITO_ECUADOR'
  | 'LLEGO_BODEGA_ECUADOR'
  | 'LISTO_ENTREGA'
  | 'ENTREGADO'
  | 'CANCELADO'

export type PaymentMethod = 'TRANSFERENCIA' | 'PAYPAL'

export type ShippingOption = 'PICKUP' | 'UBER' | 'SERVIENTREGA'

export interface StatusHistoryItem {
  id: number
  status: OrderStatus
  createdAt: string
}

export interface Order {
  id: number
  userId: number
  userName: string
  userEmail: string
  productName: string
  productAsin: string
  productPrice: number
  weight: number
  shippingOption: ShippingOption
  status: OrderStatus
  paymentMethod?: PaymentMethod
  paymentReference?: string
  totalPrice: number
  notes: string
  amazonUrl: string
  createdAt: string
  updatedAt: string
  statusHistory: StatusHistoryItem[]
}

export interface CreateOrderRequest {
  productName?: string
  productAsin?: string
  productPrice: number
  weight?: number
  shippingOption: ShippingOption
  paymentMethod: PaymentMethod
  paymentReference?: string
  securityCode: string
  amazonUrl?: string
  notes?: string
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus
  notes?: string
}

// Pricing types
export interface PricingCalculation {
  productPrice: number
  taxes: number
  handling: number
  customs: number
  shippingCost: number
  totalPrice: number
  categoryC: boolean
  categoryCMessage: string | null
  shippingDescription: string
}

export interface PricingRequest {
  productPrice: number
  weight?: number
  shippingOption?: string
}

// API Response
export interface ApiError {
  timestamp: string
  message: string
  status: number
  errors?: Record<string, string>
}

// Calculator Settings
export interface CalculatorSettings {
  TAX_RATE: string
  HANDLING_RATE: string
  SHIPPING_RATE_PER_LB: string
  MAX_SHIPPING: string
  CUSTOMS_FEE: string
  MAX_WEIGHT_FOR_STANDARD: string
  MAX_PRICE_FOR_STANDARD: string
  WHATSAPP_NUMBER: string
  BANK_NAME: string
  BANK_ACCOUNT_NAME: string
  BANK_ACCOUNT_NUMBER: string
  BANK_ACCOUNT_TYPE: string
  PAYPAL_EMAIL: string
  PAYPAL_CLIENT_ID: string
}

export interface UpdateSettingsRequest {
  taxRate?: string
  handlingRate?: string
  shippingRatePerLb?: string
  maxShipping?: string
  customsFee?: string
  maxWeightForStandard?: string
  maxPriceForStandard?: string
  whatsappNumber?: string
  bankName?: string
  bankAccountName?: string
  bankAccountNumber?: string
  bankAccountType?: string
  paypalEmail?: string
  paypalClientId?: string
}

// Amazon Scraper
export interface ProductInfo {
  title: string
  price: string
  asin: string
  image?: string
  weight?: number
  weightUnit?: string
}

export interface AmazonScrapeRequest {
  url: string
}

export interface AmazonScrapeResponse {
  success: boolean
  data?: ProductInfo
  error?: string
}
