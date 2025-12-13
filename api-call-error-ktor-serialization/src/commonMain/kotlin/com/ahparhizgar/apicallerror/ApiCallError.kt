package com.ahparhizgar.apicallerror

class ApiCallError(message: String = "API call error"): Exception(message)
class NetworkError(message: String = "Network error"): Exception(message)
