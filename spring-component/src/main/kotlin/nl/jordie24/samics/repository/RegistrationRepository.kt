package nl.jordie24.samics.repository

import nl.jordie24.samics.model.RegistrationModel
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface RegistrationRepository : MongoRepository<RegistrationModel, ObjectId>
