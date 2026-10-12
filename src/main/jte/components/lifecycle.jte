@import ua.edu.ukma.oss.web.support.PageContext
@import ua.edu.ukma.oss.web.view.ProtocolPage
@param PageContext ctx
@param ProtocolPage page
!{int stage = page.protocol().status().ordinal();}
!{boolean published = stage == 2;}
<div class="lifecycle" aria-label="Життєвий цикл протоколу">
    <div class="step done">
        <div class="dot">✓</div>
        <div class="name">Засідання</div>
        <div class="when">${ctx.fmt().date(page.meeting().scheduledAt())}</div>
    </div>
    <div class="step ${published || stage > 0 ? "done" : "current draft"}">
        <div class="dot">${published || stage > 0 ? "✓" : "2"}</div>
        <div class="name">Чернетка</div>
        <div class="when">${ctx.fmt().date(page.protocol().createdAt())}</div>
    </div>
    <div class="step ${published || stage > 1 ? "done" : (stage == 1 ? "current" : "pending")}">
        <div class="dot">${published || stage > 1 ? "✓" : "3"}</div>
        <div class="name">Затверджено</div>
        <div class="when">
            @if(page.protocol().approvedAt() != null)
                ${ctx.fmt().date(page.protocol().approvedAt())}@if(page.approvedByName() != null), ${page.approvedByName()}@endif
            @else
                очікує
            @endif
        </div>
    </div>
    <div class="step ${published ? "done" : "pending"}">
        <div class="dot">${published ? "✓" : "4"}</div>
        <div class="name">Опубліковано</div>
        <div class="when">
            @if(page.protocol().publishedAt() != null)
                ${ctx.fmt().date(page.protocol().publishedAt())}@if(page.publishedByName() != null), ${page.publishedByName()}@endif
            @else
                очікує
            @endif
        </div>
    </div>
</div>
