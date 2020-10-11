package nl.jordie24.samics.model

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.MongoId

@Document
data class RegistrationModel(
        @MongoId var calendarId: ObjectId?,
        @Field("smbw") var syncedMonthsBackwards: Long,
        @Field("smfw") var syncedMonthsForwards: Long,
        @Field("user") var username: String,
        @Field("pwd") var password: String,
        @Field("pwd_salt") var salt: String
)
