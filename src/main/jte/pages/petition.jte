@import ua.edu.ukma.oss.organization.OrganizationDto
@import ua.edu.ukma.oss.petition.PetitionDto
@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@param PageContext ctx
@param PetitionDto petition
@param OrganizationDto org
@param String meetingTitle = null
!{String code = org.code().name().toLowerCase();}
@template.layout.page(ctx = ctx, title = petition.title(), section = code, content = @`
    <div class="form-narrow" style="max-width:720px">
        <div class="breadcrumbs"><a href="/orgs/${code}">${org.name()}</a> · Петиції</div>
        <div class="page-head">
            <h1 style="margin:0">${petition.title()}</h1>
            @template.components.chip(css = Labels.petitionCss(petition.status()), text = Labels.petitionStatus(petition.status()))
        </div>
        <div class="card">
            <p class="muted small">Подано ${ctx.fmt().date(petition.createdAt())} · адресовано: ${org.name()}</p>
            <div class="petition-text">${petition.text()}</div>
            @if(petition.rejectionReason() != null)
                <p><strong>Причина відхилення:</strong> ${petition.rejectionReason()}</p>
            @endif
            @if(petition.meetingId() != null)
                <p class="small">Розглядається на засіданні: <a href="/meetings/${petition.meetingId()}">${meetingTitle}</a></p>
            @endif
        </div>
        <p style="margin-top:1rem"><a href="/orgs/${code}">← Назад до сторінки ${org.name()}</a></p>
    </div>
`)
