/**
 * 简历数据转PDF导出（动态导入，减小初始包体积）
 * @param {Object} resumeData - 简历数据
 * @param {Object} options - 配置选项
 */
export async function exportResumeToPdf(resumeData, options = {}) {
  // 动态导入 PDF 库（仅在需要时加载）
  const [{ default: html2canvas }, { default: jsPDF }] = await Promise.all([
    import('html2canvas'),
    import('jspdf')
  ])

  const {
    filename = `简历-${resumeData.name || '未知'}-${Date.now()}.pdf`,
    scale = 2
  } = options

  // 创建隐藏的渲染容器
  const container = document.createElement('div')
  container.style.cssText = `
    position: absolute;
    left: -9999px;
    top: 0;
    width: 210mm;
    padding: 15mm;
    font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
    color: #333;
    background: #fff;
    box-sizing: border-box;
  `
  container.innerHTML = generateResumeHTML(resumeData)
  document.body.appendChild(container)

  try {
    const canvas = await html2canvas(container, {
      scale,
      useCORS: true,
      logging: false,
      backgroundColor: '#ffffff'
    })

    const pdf = new jsPDF('p', 'mm', 'a4')
    const imgWidth = 210
    const pageHeight = 297
    const imgHeight = (canvas.height * imgWidth) / canvas.width
    let heightLeft = imgHeight
    let position = 0

    // 第一页
    pdf.addImage(canvas.toDataURL('image/png'), 'PNG', 0, position, imgWidth, imgHeight)
    heightLeft -= pageHeight

    // 多页处理
    while (heightLeft > 0) {
      position = heightLeft - imgHeight
      pdf.addPage()
      pdf.addImage(canvas.toDataURL('image/png'), 'PNG', 0, position, imgWidth, imgHeight)
      heightLeft -= pageHeight
    }

    pdf.save(filename)
  } finally {
    document.body.removeChild(container)
  }
}

/**
 * 生成简历HTML
 */
function generateResumeHTML(data) {
  const {
    name = '未知',
    age,
    phone,
    email,
    education,
    skills,
    experience,
    expectedSalary,
    workExperience,
    selfIntroduction,
    categoryName
  } = data

  // 解析技能
  let skillsArray = []
  if (skills) {
    try {
      skillsArray = typeof skills === 'string' ? JSON.parse(skills) : skills
    } catch {
      skillsArray = skills.split(/[,，、;；\s]+/).filter(s => s.trim())
    }
  }

  // 解析工作经历
  let workExpArray = []
  if (workExperience) {
    try {
      workExpArray = typeof workExperience === 'string' ? JSON.parse(workExperience) : workExperience
    } catch {
      // 解析失败
    }
  }

  return `
    <div style="max-width: 100%;">
      <!-- 头部信息 -->
      <div style="text-align: center; border-bottom: 2px solid #409EFF; padding-bottom: 12px; margin-bottom: 16px;">
        <h1 style="margin: 0 0 6px 0; font-size: 26px; color: #333; font-weight: bold;">${name}</h1>
        <p style="margin: 0; font-size: 13px; color: #666; line-height: 1.6;">
          ${age ? `${age}岁 | ` : ''}${education || ''} ${categoryName ? `| ${categoryName}` : ''}
          ${expectedSalary ? `<br>期望薪资：¥${expectedSalary.toLocaleString()}/月` : ''}
        </p>
        ${phone || email ? `<p style="margin: 4px 0 0 0; font-size: 12px; color: #888;">${phone ? `📱 ${phone}` : ''} ${email ? `| ✉️ ${email}` : ''}</p>` : ''}
      </div>

      <!-- 技能 -->
      ${skillsArray.length > 0 ? `
        <div style="margin-bottom: 14px;">
          <h3 style="font-size: 15px; color: #409EFF; border-left: 3px solid #409EFF; padding-left: 8px; margin: 0 0 8px 0;">专业技能</h3>
          <div style="display: flex; flex-wrap: wrap; gap: 6px;">
            ${skillsArray.map(s => `<span style="display: inline-block; background: #ecf5ff; color: #409EFF; padding: 3px 10px; border-radius: 3px; font-size: 12px;">${s}</span>`).join('')}
          </div>
        </div>
      ` : ''}

      <!-- 工作经验 -->
      ${workExpArray.length > 0 ? `
        <div style="margin-bottom: 14px;">
          <h3 style="font-size: 15px; color: #409EFF; border-left: 3px solid #409EFF; padding-left: 8px; margin: 0 0 8px 0;">工作经历</h3>
          ${workExpArray.map(exp => `
            <div style="margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px dashed #eee;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                <strong style="font-size: 14px; color: #333;">${exp.company || '未知公司'}</strong>
                <span style="font-size: 11px; color: #999;">${exp.startDate || ''} ~ ${exp.current ? '至今' : (exp.endDate || '')}</span>
              </div>
              ${exp.position ? `<div style="font-size: 12px; color: #666; margin-bottom: 4px;">${exp.position}</div>` : ''}
              ${exp.description ? `<p style="margin: 0; font-size: 12px; color: #777; line-height: 1.5;">${exp.description}</p>` : ''}
            </div>
          `).join('')}
        </div>
      ` : ''}

      <!-- 工作经验概述 -->
      ${experience && workExpArray.length === 0 ? `
        <div style="margin-bottom: 14px;">
          <h3 style="font-size: 15px; color: #409EFF; border-left: 3px solid #409EFF; padding-left: 8px; margin: 0 0 8px 0;">工作经验</h3>
          <p style="margin: 0; font-size: 13px; color: #666; line-height: 1.6;">${experience}</p>
        </div>
      ` : ''}

      <!-- 自我评价 -->
      ${selfIntroduction ? `
        <div style="margin-bottom: 14px;">
          <h3 style="font-size: 15px; color: #409EFF; border-left: 3px solid #409EFF; padding-left: 8px; margin: 0 0 8px 0;">自我评价</h3>
          <p style="margin: 0; font-size: 13px; color: #666; line-height: 1.6;">${selfIntroduction}</p>
        </div>
      ` : ''}
    </div>
  `
}
