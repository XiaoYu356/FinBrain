<template>
  <div class="risk-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>风险测评</span>
          <el-button v-if="currentResult" type="primary" @click="startAssessment">重新测评</el-button>
        </div>
      </template>

      <div v-if="currentResult && !isAssessing" class="result-container">
        <el-result
          :icon="getRiskIcon(currentResult.riskLevel)"
          :title="`您的风险等级: ${currentResult.riskLevel}`"
          :sub-title="currentResult.riskLevelDesc"
        >
          <template #extra>
            <el-descriptions :column="1" border style="max-width: 400px; margin: 0 auto;">
              <el-descriptions-item label="测评得分">{{ currentResult.score }}分</el-descriptions-item>
              <el-descriptions-item label="风险等级">{{ currentResult.riskLevel }}</el-descriptions-item>
              <el-descriptions-item label="等级描述">{{ currentResult.riskLevelDesc }}</el-descriptions-item>
              <el-descriptions-item label="测评时间">{{ formatTime(currentResult.assessmentTime) }}</el-descriptions-item>
            </el-descriptions>
          </template>
        </el-result>
      </div>

      <div v-else class="assessment-container">
        <el-steps :active="currentStep" finish-status="success" simple style="margin-bottom: 30px;">
          <el-step v-for="(q, index) in questions" :key="index" :title="`问题${index + 1}`" />
        </el-steps>

        <div v-if="currentStep < questions.length" class="question-container">
          <h3>{{ questions[currentStep].question }}</h3>
          <el-radio-group v-model="answers[currentStep]" class="options-group">
            <el-radio 
              v-for="(option, index) in questions[currentStep].options" 
              :key="index" 
              :label="option"
              border
              class="option-item"
            >
              {{ option }}
            </el-radio>
          </el-radio-group>
          <div class="action-buttons">
            <el-button v-if="currentStep > 0" @click="prevStep">上一题</el-button>
            <el-button type="primary" @click="nextStep" :disabled="!answers[currentStep]">
              {{ currentStep === questions.length - 1 ? '提交' : '下一题' }}
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRiskQuestions, submitRiskAssessment, getRiskResult } from '@/api/risk'
import { ElMessage } from 'element-plus'

const questions = ref([])
const answers = ref([])
const currentStep = ref(0)
const isAssessing = ref(false)
const currentResult = ref(null)
const loading = ref(false)

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const getRiskIcon = (level) => {
  const icons = {
    C1: 'success',
    C2: 'success',
    C3: 'warning',
    C4: 'warning',
    C5: 'error'
  }
  return icons[level] || 'info'
}

const fetchQuestions = async () => {
  try {
    const res = await getRiskQuestions()
    questions.value = res.data
    answers.value = new Array(questions.value.length).fill('')
  } catch (e) {
    console.error(e)
  }
}

const fetchResult = async () => {
  try {
    const res = await getRiskResult()
    currentResult.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const startAssessment = () => {
  isAssessing.value = true
  currentStep.value = 0
  answers.value = new Array(questions.value.length).fill('')
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const nextStep = async () => {
  if (currentStep.value < questions.value.length - 1) {
    currentStep.value++
  } else {
    await handleSubmit()
  }
}

const handleSubmit = async () => {
  loading.value = true
  try {
    const answerList = questions.value.map((q, index) => ({
      question: q.question,
      answer: answers.value[index]
    }))
    
    const res = await submitRiskAssessment({ answers: answerList })
    currentResult.value = res.data
    isAssessing.value = false
    ElMessage.success('测评完成')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchQuestions()
  fetchResult()
})
</script>

<style scoped>
.risk-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-container {
  padding: 40px 0;
}

.assessment-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.question-container {
  text-align: center;
}

.question-container h3 {
  margin-bottom: 30px;
  color: #303133;
}

.options-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 30px;
}

.option-item {
  width: 100%;
  padding: 15px 20px;
  margin: 0;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
}
</style>
