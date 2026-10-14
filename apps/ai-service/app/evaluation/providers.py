import json
import os
import urllib.request
import urllib.error
from typing import Protocol
from .schema import EvaluationRequest, Feedback
from .prompts import SYSTEM_PROMPT

class ProviderUnavailable(Exception): pass
class ProviderTransient(Exception): pass
class ProviderInvalid(Exception): pass

class EvaluationProvider(Protocol):
    def evaluate(self, request: EvaluationRequest) -> tuple[dict, dict]: ...

class OpenAIProvider:
    def evaluate(self, request: EvaluationRequest) -> tuple[dict, dict]:
        key = os.environ.get('WRITING_AI_API_KEY')
        model = os.environ.get('WRITING_AI_MODEL')
        if not key or not model:
            raise ProviderUnavailable()
        payload = {'model':model, 'temperature':0.1, 'max_tokens':5000,
                   'response_format':{'type':'json_schema','json_schema':{'name':'writing_feedback','strict':True,'schema':Feedback.model_json_schema()}},
                   'messages':[{'role':'system','content':SYSTEM_PROMPT}, {'role':'user','content':json.dumps(request.model_dump())}]}
        # Fixed HTTPS destination prevents configurable endpoints from becoming an SSRF vector.
        call = urllib.request.Request('https://api.openai.com/v1/chat/completions', data=json.dumps(payload).encode(),
                                      headers={'Authorization':'Bearer '+key, 'Content-Type':'application/json', 'Idempotency-Key':request.evaluationId})
        try:
            with urllib.request.urlopen(call, timeout=35) as response:
                raw = response.read(262145)
            if len(raw)>262144: raise ProviderInvalid()
            result = json.loads(raw)
            feedback = json.loads(result['choices'][0]['message']['content'])
            usage = result.get('usage', {})
            return feedback, {'provider':'openai','model':result.get('model',model),'usage':{'inputTokens':usage.get('prompt_tokens'),'outputTokens':usage.get('completion_tokens'),'estimatedCost':None}}
        except urllib.error.HTTPError as error:
            if error.code in (429,500,502,503,504): raise ProviderTransient() from None
            if error.code in (401,403): raise ProviderUnavailable() from None
            raise ProviderInvalid() from None
        except (TimeoutError, OSError): raise ProviderTransient() from None
        except (ValueError, KeyError, IndexError): raise ProviderInvalid() from None

class TestProvider:
    """Deterministic test double. Never available in a production environment."""
    def evaluate(self, request: EvaluationRequest) -> tuple[dict, dict]:
        span = request.response[:min(len(request.response),60)]
        return {'estimated':True,'proficiencyLevel':'TEST_ONLY','strengths':['Test fixture'], 'weaknesses':[],
                'explanation':'Simulated evaluation for automated tests only.',
                'criteria':[{'id':c.id,'score':3,'strengths':['Test fixture'],'weaknesses':[], 'evidence':[span],'improvements':['Test recommendation']} for c in request.rubric.criteria],
                'findings':[], 'improvedResponse':request.response, 'recommendations':[{'text':'Test recommendation','evidence':span}], 'flags':['TEST_ONLY']}, {'provider':'test','model':'deterministic-test-v1','usage':{'inputTokens':0,'outputTokens':0,'estimatedCost':None}}

def get_provider() -> EvaluationProvider:
    name=os.environ.get('WRITING_AI_PROVIDER','disabled')
    if name=='openai': return OpenAIProvider()
    if name=='anthropic': return AnthropicProvider()
    if name=='test' and os.environ.get('APP_ENV')=='test': return TestProvider()
    raise ProviderUnavailable()

class AnthropicProvider:
    def evaluate(self, request: EvaluationRequest) -> tuple[dict, dict]:
        key=os.environ.get('WRITING_AI_API_KEY');model=os.environ.get('WRITING_AI_MODEL')
        if not key or not model: raise ProviderUnavailable()
        payload={'model':model,'max_tokens':5000,'temperature':0.1,'system':SYSTEM_PROMPT,
                 'messages':[{'role':'user','content':json.dumps({'request':request.model_dump(),'requiredSchema':Feedback.model_json_schema()})}]}
        call=urllib.request.Request('https://api.anthropic.com/v1/messages',data=json.dumps(payload).encode(),
                                    headers={'x-api-key':key,'anthropic-version':'2023-06-01','Content-Type':'application/json'})
        try:
            with urllib.request.urlopen(call,timeout=35) as response: raw=response.read(262145)
            if len(raw)>262144: raise ProviderInvalid()
            result=json.loads(raw);parts=[p['text'] for p in result['content'] if p['type']=='text']
            feedback=json.loads(''.join(parts));usage=result.get('usage',{})
            return feedback,{'provider':'anthropic','model':result.get('model',model),'usage':{'inputTokens':usage.get('input_tokens'),'outputTokens':usage.get('output_tokens'),'estimatedCost':None}}
        except urllib.error.HTTPError as error:
            if error.code in (429,500,502,503,504,529): raise ProviderTransient() from None
            if error.code in (401,403): raise ProviderUnavailable() from None
            raise ProviderInvalid() from None
        except (TimeoutError,OSError): raise ProviderTransient() from None
        except (ValueError,KeyError,IndexError): raise ProviderInvalid() from None
