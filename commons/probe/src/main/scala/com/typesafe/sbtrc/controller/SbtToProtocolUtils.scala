package com.typesafe.sbtrc

/** Helpers to map from sbt types into serializable json types. */
object SbtToProtocolUtils {
  
  
  def manifestToProtocol[T](mf: Manifest[T]): protocol.TypeInfo =
    protocol.TypeInfo.fromManifest(mf)
  
  def keyToProtocol[T](key: sbt.AttributeKey[T]): protocol.AttributeKey =
    protocol.AttributeKey(
      key.label,
      manifestToProtocol(key.manifest)
    )
    
    
  def scopeToProtocol(scope: sbt.Scope): protocol.SbtScope = {
    // TODO - Is it acceptable to use configs *just by the name* or do we need
    // to actually look up the sbt instance itself.
    // if any code is using referential equality, we may need to re-evaluate this.
    val configString: Option[String] = 
      scope.config.toOption.map(_.name)  
    
    // We can only do this all at once, or not at all
    
    val (build, project) =
      scopedReferenceToBuildAndProject(scope.project)

    val taskString: Option[protocol.AttributeKey] =
      scope.task.toOption.map(x => keyToProtocol(x))
      
    // TODO - Build.... (probably from project reference)...
    protocol.SbtScope(
        build  = build,
        config = configString,
        project = project,
        task = taskString)
  }
  
  def projectRefToProtocol(x: sbt.ProjectRef): protocol.ProjectReference = {
    protocol.ProjectReference(x.build, x.project)
  }
  
  def projectRefFromProtocol(x: protocol.ProjectReference): sbt.ProjectRef = {
    sbt.ProjectRef(x.build, x.name)
  }
  
  def scopedReferenceToBuildAndProject(axis: sbt.ScopeAxis[sbt.Reference]): (Option[java.net.URI], Option[protocol.ProjectReference]) = { 
    import sbt._
    axis.toOption map {
      case x: ProjectRef =>
        val build = x.build
        Some(build) -> Some(projectRefToProtocol(x))
      case x: BuildRef =>
        Some(x.build) -> None
      case _ => throw new RuntimeException("was expected project/build reference, got: " + axis)
    } getOrElse (None -> None)
  }
  
  def scopedKeyToProtocol[T](key: sbt.ScopedKey[T]): protocol.ScopedKey =
    protocol.ScopedKey(
      key = keyToProtocol(key.key),
      scope = scopeToProtocol(key.scope)
    )
}
