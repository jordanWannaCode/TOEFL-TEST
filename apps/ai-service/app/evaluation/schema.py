from typing import Literal
from pydantic import BaseModel, ConfigDict, Field, model_validator

class StrictModel(BaseModel):
    model_config = ConfigDict(extra='forbid')

class Criterion(StrictModel):
    id: str = Field(min_length=1, max_length=80)
    weight: float = Field(gt=0, le=100)

class Rubric(StrictModel):
    id: str
    version: int = Field(gt=0)
    criteria: list[Criterion] = Field(min_length=1, max_length=12)
    scale: dict[str, int]
    source: Literal['CUSTOM_PEDAGOGICAL']
    officialWeights: Literal[False]
    levelDescription: str

class EvaluationRequest(StrictModel):
    evaluationId: str
    taskType: Literal['WRITE_AN_EMAIL', 'WRITE_FOR_AN_ACADEMIC_DISCUSSION']
    question: dict
    response: str = Field(min_length=1, max_length=12000)
    rubric: Rubric
    promptVersion: Literal['writing-feedback-v1']

class CriterionResult(StrictModel):
    id: str
    score: float = Field(ge=0, le=5)
    strengths: list[str] = Field(max_length=8)
    weaknesses: list[str] = Field(max_length=8)
    evidence: list[str] = Field(min_length=1, max_length=8)
    improvements: list[str] = Field(min_length=1, max_length=8)

class Finding(StrictModel):
    originalSpan: str = Field(min_length=1)
    explanation: str
    correction: str
    category: Literal['GRAMMAR','TENSE','AGREEMENT','STRUCTURE','VOCABULARY','REPETITION','CONNECTOR','REGISTER']
    severity: Literal['LOW','MEDIUM','HIGH']

class Recommendation(StrictModel):
    text: str
    evidence: str = Field(min_length=1)

class Feedback(StrictModel):
    estimated: Literal[True]
    proficiencyLevel: str
    strengths: list[str]
    weaknesses: list[str]
    explanation: str
    criteria: list[CriterionResult] = Field(min_length=1, max_length=12)
    findings: list[Finding] = Field(max_length=60)
    improvedResponse: str = Field(max_length=18000)
    recommendations: list[Recommendation] = Field(max_length=12)
    flags: list[str] = Field(max_length=12)

    def validate_against(self, request: EvaluationRequest) -> dict:
        expected = {c.id for c in request.rubric.criteria}
        if len(self.criteria) != len(expected) or {c.id for c in self.criteria} != expected:
            raise ValueError('Criterion set mismatch')
        for c in self.criteria:
            if any(not span or span not in request.response for span in c.evidence):
                raise ValueError('Unsupported criterion evidence')
        for f in self.findings:
            if f.originalSpan not in request.response:
                raise ValueError('Unsupported language finding')
        for r in self.recommendations:
            if r.evidence not in request.response:
                raise ValueError('Unsupported recommendation')
        weights = {c.id:c.weight for c in request.rubric.criteria}
        score = sum(c.score * weights[c.id] for c in self.criteria) / sum(weights.values())
        value = self.model_dump()
        value['internalScore'] = round(score, 2)
        value['scoreMax'] = 5
        if not self.findings and any(c.id == 'grammar' and c.score < 2 for c in self.criteria):
            value['flags'] = list(set(value['flags'] + ['CONTRADICTORY_GRAMMAR_FEEDBACK']))
        return value
