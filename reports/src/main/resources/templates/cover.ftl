<!DOCTYPE html>
<html>
<head>
	<title>Report</title>
	<style>
		<#include "css/common.css">
	</style>
	<style>
		@font-face {
			font-family: 'Inter';
			src: url('/static/Inter.ttf') format('truetype');
		}

		.logo-header {
			margin-left: 56px;
			margin-top: 120px;
		}

		.logo-header img {
			width: auto;
			height: 120px;
		}

		.report-title {
			margin-left: 48px;
			margin-top: 64px;
			font-size: 60px;
			line-height: 1;
		}

		.footer {
			position: absolute;
			bottom: 0;
			left: 0;
			width: calc(100% - 48px);
			height: 90px;
			background-color: var(--blue-5);
			color: white;
			padding: 24px 64px;
			align-items: center;
		}

		.footer-label {
			font-size: 16px;
		}

		.logo-footer {
			margin-left: 24px;
		}

		.logo-footer img {
			width: auto;
			height: 32px;
		}
	</style>
</head>
<body>
<div class="logo-header">
	<img src="https://png.pngtree.com/png-clipart/20230423/original/pngtree-travel-logo-design-template-for-business-and-company-png-image_9077410.png" alt="logo">
</div>

<div class="flex-column report-title">
	<div>Report</div>
</div>

<div class="flex-row footer">
	<div class="footer-label">powered by</div>
	<div class="logo-footer">
		<img src="https://png.pngtree.com/png-clipart/20230423/original/pngtree-travel-logo-design-template-for-business-and-company-png-image_9077410.png" alt="logo">
	</div>
</div>
</body>
</html>
