package fi.oph.tor.schema

import fi.oph.tor.schema.generic.{OneOf, ScalaJsonSchema}
import org.json4s._
import org.json4s.reflect.TypeInfo

class SchemaBasedTraitSerializer(schema: ScalaJsonSchema) extends Serializer[AnyRef] {
  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), AnyRef] = {
    case (TypeInfo(t, _), json) if (t.isInterface && !t.getTypeName.startsWith("java") && !t.getTypeName.startsWith("scala")) => { // <- todo: recognize trait type correctly
      val fullName: String = t.getTypeName
      val schemaType: OneOf = schema.createSchemaType(fullName).asInstanceOf[OneOf]
      val empty: List[(Class[_], AnyRef)] = Nil
      val found = schemaType.types.foldLeft(empty) {
        case (found, alternative) =>
          found ++ (try {
            val klass: Class[_] = Class.forName(alternative.fullClassName)
            List((klass, json.extract[AnyRef](format, Manifest.classType(klass))))
          } catch {
            case e: Exception => Nil
          })
      }
      found.sortBy{
        _._1.getConstructors.toList(0).getParameterTypes.length // choose longest constructor if multiple matches
      }.reverse.map(_._2).headOption match {
        case Some(matching) => matching
        case None => throw new RuntimeException("No matching implementation of " + t.getSimpleName + " found for data " + json)
      }
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = PartialFunction.empty
}