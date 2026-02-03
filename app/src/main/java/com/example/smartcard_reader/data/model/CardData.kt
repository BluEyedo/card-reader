package com.example.smartcard_reader.data.model

data class CardData(
    val cid: String,
    val nameTH: String,
    val nameEN: String,
    val birthDate: String,
    val birthDateEN: String,
    val gender: String,
    val address: String,
    val issuer: String,
    val issueDate: String,
    val issueDateEN: String,
    val expireDate: String,
    val expireDateEN: String
) {
    companion object {
        fun fromMap(map: Map<String, String>): CardData {
            return CardData(
                cid = map["CID"] ?: "",
                nameTH = map["NameTH"] ?: "",
                nameEN = map["NameEN"] ?: "",
                birthDate = map["BirthDate"] ?: "",
                birthDateEN = map["BirthDateEN"] ?: "",
                gender = map["Gender"] ?: "",
                address = map["Address"] ?: "",
                issuer = map["Issuer"] ?: "",
                issueDate = map["IssueDate"] ?: "",
                issueDateEN = map["IssueDateEN"] ?: "",
                expireDate = map["ExpireDate"] ?: "",
                expireDateEN = map["ExpireDateEN"] ?: ""
            )
        }
    }

    fun toMap(): Map<String, String> {
        return mapOf(
            "CID" to cid,
            "NameTH" to nameTH,
            "NameEN" to nameEN,
            "BirthDate" to birthDate,
            "BirthDateEN" to birthDateEN,
            "Gender" to gender,
            "Address" to address,
            "Issuer" to issuer,
            "IssueDate" to issueDate,
            "IssueDateEN" to issueDateEN,
            "ExpireDate" to expireDate,
            "ExpireDateEN" to expireDateEN
        )
    }
}