package kai.storage.fs

import kai.domain.finding.BugSignature
import kai.domain.finding.Finding
import kai.domain.finding.FindingEvidence
import kai.domain.finding.FindingKind
import kai.domain.finding.FindingStatus
import kai.domain.finding.PendingFinding
import kai.domain.id.FindingId
import kai.domain.id.OracleId
import kai.domain.id.TestCaseId
import kai.domain.observations.ArtifactKind
import kai.domain.observations.CompilationArtifact
import kai.domain.observations.CompilerDiagnostic
import kai.domain.observations.DiagnosticSeverity
import kai.domain.observations.ExecutionResult
import kai.domain.observations.Observations

object FindingCodec {
    fun encode(finding: Finding): JsonValue {
        return JsonValue.Obj(
            linkedMapOf(
                "id" to JsonValue.Str(finding.id.value),
                "status" to JsonValue.Str(finding.status.name),
                "pending" to encodePending(finding),
                "reduced" to encodeReduced(finding)
            )
        )
    }

    fun decode(value: JsonValue): Finding {
        val pendingNode = value.get("pending")
        val reducedNode = value.asObject().values["reduced"]
        return Finding(
            id = FindingId(value.string("id")),
            pending = decodePending(pendingNode),
            reduced = if (reducedNode == null || reducedNode is JsonValue.Nil) null else TestCaseCodec.decode(reducedNode),
            status = FindingStatus.valueOf(value.string("status"))
        )
    }

    private fun encodePending(finding: Finding): JsonValue {
        val pending = finding.pending
        return JsonValue.Obj(
            linkedMapOf(
                "oracleId" to JsonValue.Str(pending.oracleId.value),
                "kind" to JsonValue.Str(pending.kind.name),
                "signature" to JsonValue.Obj(
                    linkedMapOf(
                        "hash" to JsonValue.Str(pending.signature.hash),
                        "profileName" to JsonValue.Str(pending.signature.profileName),
                        "frames" to JsonValue.Arr(pending.signature.normalizedFrames.map { JsonValue.Str(it) })
                    )
                ),
                "evidence" to JsonValue.Obj(
                    linkedMapOf(
                        "description" to JsonValue.Str(pending.evidence.description),
                        "stackTrace" to optionalString(pending.evidence.stackTrace),
                        "divergence" to optionalString(pending.evidence.divergence)
                    )
                ),
                "observations" to encodeObservations(pending.observations),
                "testCase" to TestCaseCodec.encode(pending.testCase)
            )
        )
    }

    private fun encodeReduced(finding: Finding): JsonValue {
        val reduced = finding.reduced ?: return JsonValue.Nil
        return TestCaseCodec.encode(reduced)
    }

    private fun decodePending(value: JsonValue): PendingFinding {
        val signatureNode = value.obj("signature")
        val evidenceNode = value.obj("evidence")
        return PendingFinding(
            oracleId = OracleId(value.string("oracleId")),
            kind = FindingKind.valueOf(value.string("kind")),
            signature = BugSignature(
                hash = signatureNode.values.getValue("hash").asString(),
                kind = FindingKind.valueOf(value.string("kind")),
                profileName = signatureNode.values.getValue("profileName").asString(),
                normalizedFrames = signatureNode.values.getValue("frames").asArray().map { it.asString() }
            ),
            observations = decodeObservations(value.get("observations")),
            testCase = TestCaseCodec.decode(value.get("testCase")),
            evidence = FindingEvidence.create(
                description = evidenceNode.values.getValue("description").asString(),
                stackTrace = optionalString(evidenceNode.values["stackTrace"]),
                divergence = optionalString(evidenceNode.values["divergence"])
            )
        )
    }

    private fun encodeObservations(observations: Observations): JsonValue {
        return JsonValue.Obj(
            linkedMapOf(
                "testCaseId" to JsonValue.Str(observations.testCaseId.value),
                "results" to JsonValue.Arr(observations.results.map { encodeResult(it) })
            )
        )
    }

    private fun decodeObservations(value: JsonValue): Observations {
        return Observations.create(
            testCaseId = TestCaseId(value.string("testCaseId")),
            results = value.array("results").map { decodeResult(it) }
        )
    }

    private fun encodeResult(result: ExecutionResult): JsonValue {
        return JsonValue.Obj(
            linkedMapOf(
                "profileName" to JsonValue.Str(result.profileName),
                "command" to JsonValue.Arr(result.command.map { JsonValue.Str(it) }),
                "exitCode" to JsonValue.Num(result.exitCode.toLong()),
                "stdout" to JsonValue.Str(result.stdout),
                "stderr" to JsonValue.Str(result.stderr),
                "durationMs" to JsonValue.Num(result.durationMs),
                "diagnostics" to JsonValue.Arr(result.diagnostics.map { diagnostic ->
                    JsonValue.Obj(
                        linkedMapOf(
                            "severity" to JsonValue.Str(diagnostic.severity.name),
                            "message" to JsonValue.Str(diagnostic.message),
                            "location" to optionalString(diagnostic.location)
                        )
                    )
                }),
                "artifacts" to JsonValue.Arr(result.artifacts.map { artifact ->
                    JsonValue.Obj(
                        linkedMapOf(
                            "kind" to JsonValue.Str(artifact.kind.name),
                            "path" to JsonValue.Str(artifact.path),
                            "sizeBytes" to JsonValue.Num(artifact.sizeBytes)
                        )
                    )
                })
            )
        )
    }

    private fun decodeResult(value: JsonValue): ExecutionResult {
        val node = value.asObject()
        return ExecutionResult.create(
            profileName = node.values.getValue("profileName").asString(),
            command = node.values.getValue("command").asArray().map { it.asString() },
            exitCode = node.values.getValue("exitCode").asLong().toInt(),
            stdout = node.values.getValue("stdout").asString(),
            stderr = node.values.getValue("stderr").asString(),
            durationMs = node.values.getValue("durationMs").asLong(),
            diagnostics = node.values.getValue("diagnostics").asArray().map { diagnostic ->
                val item = diagnostic.asObject()
                CompilerDiagnostic(
                    severity = DiagnosticSeverity.valueOf(item.values.getValue("severity").asString()),
                    message = item.values.getValue("message").asString(),
                    location = optionalString(item.values["location"])
                )
            },
            artifacts = node.values["artifacts"]?.asArray()?.map { artifact ->
                val item = artifact.asObject()
                CompilationArtifact(
                    kind = ArtifactKind.valueOf(item.values.getValue("kind").asString()),
                    path = item.values.getValue("path").asString(),
                    sizeBytes = item.values.getValue("sizeBytes").asLong()
                )
            } ?: emptyList()
        )
    }

    private fun optionalString(value: String?): JsonValue {
        return if (value == null) JsonValue.Nil else JsonValue.Str(value)
    }

    private fun optionalString(value: JsonValue?): String? {
        return if (value == null || value is JsonValue.Nil) null else value.asString()
    }
}
